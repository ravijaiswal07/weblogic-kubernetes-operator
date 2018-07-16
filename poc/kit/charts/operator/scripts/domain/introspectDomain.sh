#!/bin/bash

# Configure and start the node manager so that we can use it to encrypt the
# username and password and put them into files that can be used to connect
# to the node manager later in the server pods (so that the server pods don't
# have to mount the secret containing the username and password)
#
# This code is copied from startServer.sh and modified to not bother redirecting the
# node manager output to the domain logs directory (so that we don't have to mount
# the domain logs directory into the domain introspection pod).
#
# TBD - figure out how to share this code

function getDomainName() {
  # We need the domain name to register the domain with the node manager
  # but we only have the domain home.
  # The 'right' way to find the domain name is to use offline wlst to
  # read the domain then get it from the domain mbean,
  # but that's slow and complicated.
  # Instead, just get it by reading config.xml directly.
  # Look for the 1st occurence of
  #  <name>demo2-domain1</name>
  # then extract the domain name from it.
  export DOMAIN_NAME=`grep "^  <name>" ${DOMAIN_HOME}/config/config.xml | awk -F'<|>' '{print $3}'`
}

function createLocalNodeManagerHome() {
  local_nmdir=$1

  # Create nodemanager home directory that is local to the k8s node
  mkdir -p ${local_nmdir}

  # totally take control of the node manager configuration
  nm_props=${local_nmdir}/nodemanager.properties
  nm_domains=${local_nmdir}/nodemanager.domains

  echo "#Node manager properties" > ${nm_props}
  echo "DomainsFile=${nm_domains}" >> ${nm_props}
  echo "LogLimit=0" >> ${nm_props}
  echo "DomainsDirRemoteSharingEnabled=true" >> ${nm_props}
  echo "PropertiesVersion=12.2.1" >> ${nm_props}
  echo "AuthenticationEnabled=true" >> ${nm_props}
  echo "NodeManagerHome=${local_nmdir}" >> ${nm_props}
  echo "JavaHome=/usr/java/jdk1.8.0_151/Contents/Home" >> ${nm_props}
  echo "LogLevel=FINEST" >> ${nm_props}
  echo "DomainsFileEnabled=true" >> ${nm_props}
  echo "ListenAddress=${DOMAIN_UID}-domain-introspector" >> ${nm_props}
  echo "NativeVersionEnabled=true" >> ${nm_props}
  echo "ListenPort=5556" >> ${nm_props}
  echo "LogToStderr=true" >> ${nm_props}
  echo "weblogic.StartScriptName=startWebLogic.sh" >> ${nm_props}
  echo "SecureListener=false" >> ${nm_props}
  echo "LogCount=1" >> ${nm_props}
  echo "QuitEnabled=false" >> ${nm_props}
  echo "LogAppend=true" >> ${nm_props}
  echo "weblogic.StopScriptEnabled=false" >> ${nm_props}
  echo "StateCheckInterval=500" >> ${nm_props}
  echo "CrashRecoveryEnabled=true" >> ${nm_props}
  echo "weblogic.StartScriptEnabled=false" >> ${nm_props}
  echo "LogFormatter=weblogic.nodemanager.server.LogFormatter" >> ${nm_props}
  echo "ListenBacklog=50" >> ${nm_props}

  echo "#Domains and directories created by Configuration Wizard" > ${nm_domains}
  echo "${DOMAIN_NAME}=${DOMAIN_HOME}" >> ${nm_domains}

  cp ${DOMAIN_HOME}/bin/startNodeManager.sh ${local_nmdir}
  sed -i -e "s:${DOMAIN_HOME}/nodemanager:${local_nmdir}:g" ${local_nmdir}/startNodeManager.sh
}

function startNodeManager() {
  # TBD - add error handling

  # Create a node manager home local to this pod
  local_nmdir=/u01/nodemanager
  createLocalNodeManagerHome ${local_nmdir}
  export JAVA_PROPERTIES="-DNodeManagerHome=${local_nmdir}"
  export NODEMGR_HOME="${local_nmdir}"

  echo "Start the nodemanager"
  . ${NODEMGR_HOME}/startNodeManager.sh &

  echo "Allow the nodemanager some time to start before attempting to connect"
  sleep 15
  echo "Finished waiting for the nodemanager to start"
}

function introspectDomain() {
  echo "Introspecting the domain"
  export OUTPUT_DIR="/u01"
  export TOPOLOGY_YAML="${OUTPUT_DIR}/topology.yaml"
  export SERVER_CM_YAML="${OUTPUT_DIR}/server-cm.yaml"
  export SERVER_SECRET_YAML="${OUTPUT_DIR}/server-secret.yaml"
  export SECRETS_DIR="/weblogic-operator/secrets/"

  wlst.sh -skipWLSModuleScanning /weblogic-operator/scripts/introspect-domain.py
  exitcode="$?"
  if [ $exitcode != 0 ]; then
    echo "Domain introspection unexpectedly failed"
    exit $exitcode
  fi
  echo "Finished introspecting the domain"
}

getDomainName
startNodeManager
introspectDomain

echo "Wait indefinitely so that the Kubernetes pod does not exit and try to restart"
while true; do sleep 60; done
exit 0
