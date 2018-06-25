#!/bin/bash

set -x

export THIS_DIR=`dirname ${BASH_SOURCE[0]}`

export DOMAINS_NAMESPACE=$1
export DOMAIN_UID=$2
export POD_TEMPLATE=$3
export SERVICE_TEMPLATE=$4
export ADMIN_SERVER_NAME=$5

# simulate the operator stopping the admin server

# remove the admin server's t3 service
export T3_SERVICE_YAML=${DOMAINS_NAMESPACE}-${DOMAIN_UID}-${SERVICE_TEMPLATE}-${ADMIN_SERVER_NAME}-admin-server-t3-service.yaml
kubectl delete -f ${T3_SERVICE_YAML}
rm ${T3_SERVICE_YAML}

# remove the admin server's service
export SERVICE_YAML=${DOMAINS_NAMESPACE}-${DOMAIN_UID}-${SERVICE_TEMPLATE}-${ADMIN_SERVER_NAME}-admin-server-service.yaml
kubectl delete -f ${SERVICE_YAML}
rm ${SERVICE_YAML}

# remove the admin server's pod
export POD_YAML=${DOMAINS_NAMESPACE}-${DOMAIN_UID}-${POD_TEMPLATE}-${ADMIN_SERVER_NAME}-admin-server-pod.yaml
export POD=${DOMAIN_UID}-${ADMIN_SERVER_NAME}
kubectl delete -f ${POD_YAML}
${THIS_DIR}/wait-for-pod-deleted.sh ${DOMAINS_NAMESPACE} ${POD}
rm ${POD_YAML}
