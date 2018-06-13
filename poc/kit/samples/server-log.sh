#!/bin/bash

. %SETUP_ENV_SCRIPT%

#set -x

SERVER_NAME="${1}"

if [ ${POD_DOMAIN_LOGS_DIR} != "null" ]; then
  cat ${DOMAIN_LOGS_PV_DIR}/${SERVER_NAME}.log
else
  kubectl exec -n ${DOMAIN_NAMESPACE} ${DOMAIN_UID}-${SERVER_NAME} cat ${POD_DOMAIN_HOME_DIR}/servers/${SERVER_NAME}/logs/${SERVER_NAME}.log
fi
