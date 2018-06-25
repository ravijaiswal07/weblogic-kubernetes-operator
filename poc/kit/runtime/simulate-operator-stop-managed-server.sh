#!/bin/bash

set -x

export THIS_DIR=`dirname ${BASH_SOURCE[0]}`

export DOMAINS_NAMESPACE=$1
export DOMAIN_UID=$2
export MANAGED_TEMPLATE=$3
export MANAGED_SERVER_NAME=$4

# simulate the operator stopping the admin server

export MANAGED_POD_YAML=${DOMAINS_NAMESPACE}-${DOMAIN_UID}-${MANAGED_TEMPLATE}-${MANAGED_SERVER_NAME}-managed-server-pod.yaml
export MANAGED_POD=${DOMAIN_UID}-${MANAGED_SERVER_NAME}

kubectl delete -f ${MANAGED_POD_YAML}
${THIS_DIR}/wait-for-pod-deleted.sh ${DOMAINS_NAMESPACE} ${MANAGED_POD}
rm ${MANAGED_POD_YAML}

