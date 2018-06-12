#!/bin/bash

. %SETUP_ENV_SCRIPT%

#set -x

SERVER_NAME="${1}"

readyHave="false"
readyWant="true"
iter=1
while [ "${readyHave}" != "${readyWant}" -a ${iter} -lt 101 ] ; do
  readyHave=`kubectl get pod -n ${DOMAIN_NAMESPACE} ${DOMAIN_UID}-${SERVER_NAME} -o jsonpath='{.status.containerStatuses[0].ready}'`
  echo readyHave=${readyHave}
  if [ "${readyHave}" != "${readyWant}" ] ; then
    echo "waiting for ${SERVER_NAME} to start, attempt ${iter} : ready=${readyHave}"
    iter=`expr $iter + 1`
    sleep 10
  else
    echo "${SERVER_NAME} has started"
  fi
done
if [ "${readyHave}" != "${readyWant}" ] ; then
  echo "warning: ${SERVER_NAME} still has not started: ready=${readyHave}"
fi
