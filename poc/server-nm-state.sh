#!/bin/bash

. pocenv.sh
set -x

SERVER_NAME="${1}"

kubectl exec -n ${DOMAIN_NAMESPACE} ${DOMAIN_UID}-${SERVER_NAME} /weblogic-operator/scripts/readState.sh
