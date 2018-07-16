#!/bin/bash

#set -x

NAMESPACE=$1
POD=$2

readyHave="false"
readyWant="true"
iter=1
while [ "${readyHave}" != "${readyWant}" -a ${iter} -lt 101 ] ; do
  readyHave=`kubectl get pod -n ${NAMESPACE} ${POD} -o jsonpath='{.status.containerStatuses[0].ready}'`
  echo readyHave=${readyHave}
  if [ "${readyHave}" != "${readyWant}" ] ; then
    echo "waiting for ${POD} to start, attempt ${iter} : ready=${readyHave}"
    iter=`expr $iter + 1`
    sleep 10
  else
    echo "${POD} has started"
  fi
done
if [ "${readyHave}" != "${readyWant}" ] ; then
  echo "warning: ${POD} still has not started: ready=${readyHave}"
fi
