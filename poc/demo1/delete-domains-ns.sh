#!/bin/bash

. ./domains-ns-env.sh

set -x

#----------------------------------------------------------------------------------------
# All/most customers need these functions as-is
#----------------------------------------------------------------------------------------

function verifyNoDomainsPresent {
  # TBD - if any domains are present in the namespace, warn and return false
  return true
}

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

function deleteKubernetesResourcesBase {
  deleteResource cm weblogic-domain-cm
  deleteGlobalResource ns ${DOMAINS_NAMESPACE}
}

function deleteGeneratedFilesBase {
  rm ${GENERATED_FILES}/domain-cm.yaml
}

#----------------------------------------------------------------------------------------
# Functionality specific to this domains namespace
#----------------------------------------------------------------------------------------

function deleteKubernetesResources {
  deleteKubernetesResourcesBase
}

function deleteGeneratedFiles {
  deleteGeneratedFilesBase
}

function main {
  if [ verifyNoDomainsPresent == false ]; then
    return
  fi
  # attempt to delete all resources for this domain namespace, whether or not they already
  # exist, so that if there was a prior failure, we still have a chance to cleanup
  deleteKubernetesResources
  deleteGeneratedFiles
}

main
