#!/bin/bash

#set -x

NAMESPACE=$1
POD=$2

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

waitUntilResourceNoLongerExists pod ${POD} ${NAMESPACE}

