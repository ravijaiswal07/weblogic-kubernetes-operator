#!/bin/bash

. %SETUP_SCRIPT_ENV_SCRIPT%

#set -x

SERVER_NAME="${1}"

if [ ${POD_DOMAIN_LOGS_DIR} != "null" ]; then
  cat ${DOMAIN_LOGS_PV_DIR}/nodemanager-${SERVER_NAME}.log
else
  kubectl exec -n ${DOMAINS_NAMESPACE} ${DOMAIN_UID}-${SERVER_NAME} cat /u01/nodemanager/nodemanager.log
fi
