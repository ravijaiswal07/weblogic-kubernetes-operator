#!/bin/bash

set -x

export THIS_DIR=`dirname ${BASH_SOURCE[0]}`

export DOMAINS_NAMESPACE=$1
export DOMAIN_UID=$2
export ADMIN_TEMPLATE=$3
export ADMIN_SERVER_NAME=$4

# simulate the operator stopping the admin server

export ADMIN_POD_YAML=${DOMAINS_NAMESPACE}-${DOMAIN_UID}-${ADMIN_TEMPLATE}-${ADMIN_SERVER_NAME}-admin-server-pod.yaml
export ADMIN_POD=${DOMAIN_UID}-${ADMIN_SERVER_NAME}

kubectl delete -f ${ADMIN_POD_YAML}
${THIS_DIR}/wait-for-pod-deleted.sh ${DOMAINS_NAMESPACE} ${ADMIN_POD}
rm ${ADMIN_POD_YAML}

