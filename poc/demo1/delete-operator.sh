#!/bin/bash

. ./operator-env.sh

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
  kubectl delete ${type} ${name} -n ${OPERATOR_NAMESPACE} 
  waitUntilResourceNoLongerExists ${type} ${name} ${OPERATOR_NAMESPACE}
}

function deleteKubernetesResourcesBase {
  deleteResource service    external-weblogic-operator-svc
  deleteResource service    internal-weblogic-operator-svc
  deleteResource deployment weblogic-operator
  deleteResource secret     weblogic-operator-secrets
  deleteResource cm         weblogic-operator-cm
  deleteResource sa         ${OPERATOR_SERVICE_ACCOUNT}
  deleteGlobalResource clusterrolebinding ${OPERATOR_NAMESPACE}-operator-rolebinding-nonresource
  deleteGlobalResource clusterrolebinding ${OPERATOR_NAMESPACE}-operator-rolebinding-discovery
  deleteGlobalResource clusterrolebinding ${OPERATOR_NAMESPACE}-operator-rolebinding-auth-delegator
  deleteGlobalResource clusterrolebinding ${OPERATOR_NAMESPACE}-operator-rolebinding
  deleteGlobalResource ns                 ${OPERATOR_NAMESPACE}
}

function deleteGeneratedFilesBase {
  rm ${GENERATED_FILES}/operator-rolebinding.yaml
  rm ${GENERATED_FILES}/operator-rolebinding-auth-delegator.yaml
  rm ${GENERATED_FILES}/operator-rolebinding-discovery.yaml
  rm ${GENERATED_FILES}/operator-rolebinding-nonresource.yaml
  rm ${GENERATED_FILES}/operator-sa.yaml
  rm ${GENERATED_FILES}/operator-cm.yaml
  rm ${GENERATED_FILES}/operator-secrets.yaml
  rm ${GENERATED_FILES}/operator-dep.yaml
  rm ${GENERATED_FILES}/operator-internal-svc.yaml
  rm ${GENERATED_FILES}/operator-external-svc.yaml
}

#----------------------------------------------------------------------------------------
# Functionality specific to this operator namespace
#----------------------------------------------------------------------------------------

function deleteKubernetesResources {
  deleteKubernetesResourcesBase
}

function deleteGeneratedFiles {
  deleteGeneratedFilesBase
}

function main {
  # attempt to delete all resources for this operator namespace, whether or not they already
  # exist, so that if there was a prior failure, we still have a chance to cleanup
  deleteKubernetesResources
  deleteGeneratedFiles
}

main
