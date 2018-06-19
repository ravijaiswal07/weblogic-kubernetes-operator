#!/bin/bash

. ./domain-env.sh

set -x

#----------------------------------------------------------------------------------------
# All/most customers need these functions as-is
#----------------------------------------------------------------------------------------

function waitUntilResourceNoLongerExists {
  type=$1
  name=$2
  ns=$3

  deleted=false
  iter=1
  while [ ${deleted} == false -a $iter -lt 101 ]; do
    if [ -z ${ns} ]; then
      kubectl get ${type} ${name}
    else
      kubectl get ${type} ${name} -n ${ns} 
    fi
    if [ $? != 0 ]; then
      deleted=true
    else
      iter=`expr $iter + 1`
      sleep 10
    fi
  done
  if [ ${deleted} == false ]; then
    if [ -z ${ns} ]; then
      echo "Warning - the ${type} ${name} still exists"
    else
      echo "Warning - the ${type} ${name} in ${ns} still exists"
    fi
  else
    if [ -z ${ns} ]; then
      echo "${type} ${name} has been deleted"
    else
      echo "${type} ${name} in ${ns} has been deleted"
    fi
  fi
}

function deleteGlobalResource {
  type=$1
  name=$2
  kubectl delete ${type} ${name}
  waitUntilResourceNoLongerExists ${type} ${name}
}

function deleteResource {
  type=$1
  name=$2
  kubectl delete ${type} ${name} -n ${DOMAINS_NAMESPACE}
  waitUntilResourceNoLongerExists ${type} ${name} ${DOMAINS_NAMESPACE}
}

function deleteServerResources {
  # TBD - should just find the pods and services by a naming pattern and delete them,
  # instead of deleting them explicitly (especially since you need the topology to
  # generically get the list server names, and since resources from servers
  # that no longer exist may still exist)
  for i in $(seq 1 $MANAGED_SERVER_COUNT); do
    MANAGED_SERVER_NAME="${MANAGED_SERVER_BASE_NAME}${i}"
    deleteResource pod     ${DOMAIN_UID}-${MANAGED_SERVER_NAME}
    deleteResource service ${DOMAIN_UID}-${MANAGED_SERVER_NAME}
  done

  deleteResource pod     ${DOMAIN_UID}-${ADMIN_SERVER_NAME}
  deleteResource service ${DOMAIN_UID}-${ADMIN_SERVER_NAME}
  deleteResource service ${DOMAIN_UID}-${ADMIN_SERVER_NAME}-extchannel-t3channel ${DOMAINS_NAMESPACE}
}

function deleteIntrospectorResources {
  deleteResource pod ${DOMAIN_UID}-introspect-domain
  deleteResource cm ${DOMAIN_UID}-weblogic-domain-bindings-cm
}

function deleteDomainCredentialsSecret {
  deleteResource secret ${DOMAIN_CREDENTIALS_SECRET_NAME}
}

function deleteKubernetesResourcesBase {
  deleteServerResources
  deleteIntrospectorResources
  deleteDomainCredentialsSecret
}

function deleteGeneratedFilesBase {
  rm ${GENERATED_FILES}/admin-server-pod.yamlt
  rm ${GENERATED_FILES}/admin-server-service.yamlt
  rm ${GENERATED_FILES}/admin-server-t3-service.yamlt

  rm ${GENERATED_FILES}/managed-server-pod.yamlt
  rm ${GENERATED_FILES}/managed-server-service.yamlt

  # TBD - should just find the pods and services by a naming pattern and delete them,
  # instead of deleting them explicitly (especially since you need the topology to
  # generically get the list of managed server names, and since resources from servers
  # that no longer exist
  rm ${GENERATED_FILES}/${ADMIN_SERVER_NAME}-pod.yaml
  rm ${GENERATED_FILES}/${ADMIN_SERVER_NAME}-service.yaml
  rm ${GENERATED_FILES}/${ADMIN_SERVER_NAME}-t3-service.yaml
  for i in $(seq 1 $MANAGED_SERVER_COUNT); do
    MANAGED_SERVER_NAME="${MANAGED_SERVER_BASE_NAME}${i}"
    rm ${GENERATED_FILES}/${MANAGED_SERVER_NAME}-pod.yaml
    rm ${GENERATED_FILES}/${MANAGED_SERVER_NAME}-service.yaml
  done

  rm ${GENERATED_FILES}/introspect-domain-pod.yaml
  rm ${GENERATED_FILES}/introspect-domain-pod.yamlt
  rm ${GENERATED_FILES}/domain-bindings-cm.yaml

  rm ${GENERATED_FILES}/server-log.sh
  rm ${GENERATED_FILES}/server-nm-log.sh
  rm ${GENERATED_FILES}/server-nm-state.sh
  rm ${GENERATED_FILES}/server-out.sh
  rm ${GENERATED_FILES}/server-pod-desc.sh
  rm ${GENERATED_FILES}/server-pod-log.sh
  rm ${GENERATED_FILES}/server-pod-state.sh
  rm ${GENERATED_FILES}/start-server.sh
  rm ${GENERATED_FILES}/stop-server.sh
  rm ${GENERATED_FILES}/wait-for-server-to-start.sh
}

#----------------------------------------------------------------------------------------
# Functionality specific to this domain
#----------------------------------------------------------------------------------------

function deleteKubernetesResources {
  deleteKubernetesResourcesBase
  deleteResource pvc ${DOMAIN_UID}-weblogic-domain-home-pvc
  deleteGlobalResource pv ${DOMAIN_UID}-weblogic-domain-home-pv
  deleteResource pvc ${DOMAIN_UID}-weblogic-domain-logs-pvc
  deleteGlobalResource pv ${DOMAIN_UID}-weblogic-domain-logs-pv
}

function deletePersistentVolumes {
  rm -rf ${DOMAIN_PVS_DIR}
}

function deleteGeneratedFiles {
  deleteGeneratedFilesBase
  rm ${GENERATED_FILES}/domain-home-pv.yaml
  rm ${GENERATED_FILES}/domain-home-pvc.yaml
  rm ${GENERATED_FILES}/domain-logs-pv.yaml
  rm ${GENERATED_FILES}/domain-logs-pvc.yaml
  rm -r ${GENERATED_FILES}/domain-home
}

function main {
  # attempt to delete all resources for this domain, whether or not they already
  # exist, so that if there was a prior failure, we still have a chance to cleanup
  deleteKubernetesResources
  deletePersistentVolumes
  deleteGeneratedFiles
}

main
