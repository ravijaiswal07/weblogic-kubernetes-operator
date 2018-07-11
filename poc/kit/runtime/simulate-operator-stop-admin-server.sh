#!/bin/bash

set -x

export THIS_DIR=`dirname ${BASH_SOURCE[0]}`

export DOMAINS_NAMESPACE=$1
export DOMAIN_UID=$2
export ADMIN_SERVER_NAME=$3

# simulate the operator stopping the admin server
export RESOURCES_YAML=${DOMAIN_UID}-admin-server.yaml
export POD=${DOMAIN_UID}-${ADMIN_SERVER_NAME}
kubectl delete -f ${RESOURCES_YAML}
${THIS_DIR}/wait-for-pod-deleted.sh ${DOMAINS_NAMESPACE} ${POD}
rm ${RESOURCES_YAML}
