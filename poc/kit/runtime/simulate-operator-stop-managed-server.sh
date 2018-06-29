#!/bin/bash

set -x

export THIS_DIR=`dirname ${BASH_SOURCE[0]}`

export DOMAINS_NAMESPACE=$1
export DOMAIN_UID=$2
export POD_TEMPLATE=$3
export SERVICE_TEMPLATE=$4
export MANAGED_SERVER_NAME=$5

# simulate the operator stopping a managed server

# remove the managed server's pod
export POD_YAML=${DOMAIN_UID}-${POD_TEMPLATE}-${MANAGED_SERVER_NAME}-server-pod.yaml
export POD=${DOMAIN_UID}-${MANAGED_SERVER_NAME}
kubectl delete -f ${POD_YAML}
${THIS_DIR}/wait-for-pod-deleted.sh ${DOMAINS_NAMESPACE} ${POD}
rm ${POD_YAML}

# remove the managed server's service
export SERVICE_YAML=${DOMAIN_UID}-${SERVICE_TEMPLATE}-${MANAGED_SERVER_NAME}-server-service.yaml
kubectl delete -f ${SERVICE_YAML}
rm ${SERVICE_YAML}
