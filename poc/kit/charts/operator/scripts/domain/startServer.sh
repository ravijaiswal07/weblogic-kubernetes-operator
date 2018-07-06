#!/bin/bash

local_nmdir=/u01/nodemanager

# Create a folder
# $1 - path of folder to create
function createFolder {
  mkdir -m 777 -p $1
  if [ ! -d $1 ]; then
    fail "Unable to create folder $1"
  fi
}

# Function to create server specific scripts and properties (e.g startup.properties, etc)
function createServerScriptsProperties() {
  local_nmdir=$1

  # Create startup.properties file
  datadir=${DOMAIN_HOME}/servers/${SERVER_NAME}/data/nodemanager
  stateFile=${datadir}/${SERVER_NAME}.state
  startProp=${datadir}/startup.properties
  if [ -f "$startProp" ]; then
    echo "startup.properties already exists"
    return 0
  fi

  createFolder ${datadir}
  echo "# Server startup properties" > ${startProp}
  echo "AutoRestart=true" >> ${startProp}
  if [ "${SERVER_NAME}" != "${ADMIN_NAME}" ]; then
    echo "AdminURL=http\://${DOMAIN_UID}-${ADMIN_NAME}\:${ADMIN_PORT}" >> ${startProp}
  fi
  echo "RestartMax=2" >> ${startProp}
  echo "RotateLogOnStartup=false" >> ${startProp}
  echo "RotationType=bySize" >> ${startProp}
  echo "RotationTimeStart=00\:00" >> ${startProp}
  echo "RotatedFileCount=100" >> ${startProp}
  echo "RestartDelaySeconds=0" >> ${startProp}
  echo "FileSizeKB=5000" >> ${startProp}
  echo "FileTimeSpanFactor=3600000" >> ${startProp}
  echo "RestartInterval=3600" >> ${startProp}
  echo "NumberOfFilesLimited=true" >> ${startProp}
  echo "FileTimeSpan=24" >> ${startProp}
  echo "NMHostName=${DOMAIN_UID}-${SERVER_NAME}" >> ${startProp}
}

# Function to create a local copy of the node manager home
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
  echo "ListenAddress=${DOMAIN_UID}-${SERVER_NAME}" >> ${nm_props}
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
  if [ ${DOMAIN_LOGS} != "null" ]; then
    # redirect the per-server node manager log if the user asked us to:
    echo "LogFile=${DOMAIN_LOGS}/nodemanager-${SERVER_NAME}.log" >> ${nm_props}
  fi
  echo "LogFormatter=weblogic.nodemanager.server.LogFormatter" >> ${nm_props}
  echo "ListenBacklog=50" >> ${nm_props}

  echo "#Domains and directories created by Configuration Wizard" > ${nm_domains}
  echo "${DOMAIN_NAME}=${DOMAIN_HOME}" >> ${nm_domains}

  cp ${DOMAIN_HOME}/bin/startNodeManager.sh ${local_nmdir}
  sed -i -e "s:${DOMAIN_HOME}/nodemanager:${local_nmdir}:g" ${local_nmdir}/startNodeManager.sh
}

# Check for stale state file and remove if found"
if [ -f "$stateFile" ]; then
  echo "Removing stale file $stateFile"
  rm ${stateFile}
fi

# Create a node manager home local to this pod
createLocalNodeManagerHome ${local_nmdir}
export JAVA_PROPERTIES="-DNodeManagerHome=${local_nmdir}"
if [ ${DOMAIN_LOGS} != "null" ]; then
  # redirect the per-server node manager log if the user asked us to:
  export JAVA_PROPERTIES="-DLogFile=${DOMAIN_LOGS}/nodemanager-${SERVER_NAME}.log ${JAVA_PROPERTIES}"
fi
export NODEMGR_HOME="${local_nmdir}"

# Create startup.properties
echo "Create startup.properties"
createServerScriptsProperties ${local_nmdir}

echo "Start the nodemanager"
. ${NODEMGR_HOME}/startNodeManager.sh &

echo "Allow the nodemanager some time to start before attempting to connect"
sleep 15
echo "Finished waiting for the nodemanager to start"

echo "Update JVM arguments"
echo "Arguments=${USER_MEM_ARGS} -XX\:+UnlockExperimentalVMOptions -XX\:+UseCGroupMemoryLimitForHeap ${JAVA_OPTIONS}" >> ${startProp}

echo "Copy the generated situational config to the domain home"
mkdir ${DOMAIN_HOME}/optconfig
cp /weblogic-operator/sitcfg/operator-situational-config.xml ${DOMAIN_HOME}/optconfig

echo "Start the server"
wlst.sh -skipWLSModuleScanning /weblogic-operator/scripts/start-server.py ${DOMAIN_UID}

cat ${DOMAIN_HOME}/servers/${SERVER_NAME}/security/boot.properties

echo "Wait indefinitely so that the Kubernetes pod does not exit and try to restart"
while true; do sleep 60; done
