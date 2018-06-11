#!/bin/bash

. %SETUP_ENV_SCRIPT%

#set -x

SERVER_NAME="${1}"

kubectl exec -n ${DOMAIN_NAMESPACE} ${DOMAIN_UID}-${SERVER_NAME} cat ${POD_DOMAIN_HOME_DIR}/servers/${SERVER_NAME}/logs/${SERVER_NAME}.out