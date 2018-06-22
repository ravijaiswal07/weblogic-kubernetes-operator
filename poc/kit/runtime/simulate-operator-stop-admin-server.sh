#!/bin/bash

set -x

export THIS_DIR=`dirname ${BASH_SOURCE[0]}`

export DOMAINS_NAMESPACE=$1
export DOMAIN_UID=$2
export ADMIN_SERVER_NAME=$3

export POD_YAML=${DOMAINS_NAMESPACE}-${DOMAIN_UID}-admin-server-pod.yaml

# simulate the operator stopping the admin server
kubectl delete -f ${POD_YAML}
${THIS_DIR}/wait-for-pod-deleted.sh ${DOMAINS_NAMESPACE} ${DOMAIN_UID}-${ADMIN_SERVER_NAME}
rm ${POD_YAML}

