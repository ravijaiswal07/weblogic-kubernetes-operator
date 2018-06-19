#!/bin/bash

. ./demo-env.sh

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
  kubectl delete ${type} ${name} -n default
  waitUntilResourceNoLongerExists ${type} ${name} default
}

function deleteKubernetesResourcesBase {
  deleteGlobalResource clusterrole weblogic-operator-cluster-role
  deleteGlobalResource clusterrole weblogic-operator-cluster-role-nonresource
  deleteGlobalResource clusterrole weblogic-operator-namespace-role
}

function deleteGeneratedFilesBase {
  rm ${GENERATED_FILES}/operator-clusterrole.yaml
  rm ${GENERATED_FILES}/operator-clusterrole-nonresource.yaml
  rm ${GENERATED_FILES}/operator-clusterrole-namespace.yaml
}

#----------------------------------------------------------------------------------------
# Functionality specific to this operator namespace
#----------------------------------------------------------------------------------------

function deleteELKIntegrationKubernetesResources {
  deleteResource deployment kibana
  deleteResource service    kibana
  deleteResource deployment elasticsearch
  deleteResource service    elasticsearch
}

function deleteKubernetesResources {
  deleteKubernetesResourcesBase
  deleteELKIntegrationKubernetesResources
}

function deleteELKIntegrationGeneratedFiles {
  rm ${GENERATED_FILES}/kibana-dep.yaml
  rm ${GENERATED_FILES}/kibana-svc.yaml
  rm ${GENERATED_FILES}/elasticsearch-dep.yaml
  rm ${GENERATED_FILES}/elasticsearch-svc.yaml
}

function deleteGeneratedFiles {
  deleteGeneratedFilesBase
  deleteELKIntegrationGeneratedFiles
}

function main {
  # attempt to delete all resources, whether or not they already
  # exist, so that if there was a prior failure, we still have a chance to cleanup
  deleteKubernetesResources
  deleteGeneratedFiles
}

main
