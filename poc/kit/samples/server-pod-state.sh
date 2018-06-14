#!/bin/bash

. %SETUP_SCRIPT_ENV_SCRIPT%

#set -x

SERVER_NAME="${1}"

kubectl get pod -n ${DOMAINS_NAMESPACE} ${DOMAIN_UID}-${SERVER_NAME}
