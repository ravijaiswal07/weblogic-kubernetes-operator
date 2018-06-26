#!/bin/bash

set -x

export THIS_DIR=`dirname ${BASH_SOURCE[0]}`

export DOMAINS_NAMESPACE=$1
export DOMAIN_UID=$2
export POD_TEMPLATE=$3
export SERVICE_TEMPLATE=$4
export SITCFG=$5
export DOMAIN_NAME=$6
export ADMIN_SERVER_NAME=$7
export ADMIN_SERVER_PORT=$8
export MANAGED_SERVER_NAME=$9
export MANAGED_SERVER_PORT=${10}

# simulate the operator starting a managed server:

# create a pod for the managed server
export POD_YAML=${DOMAINS_NAMESPACE}-${DOMAIN_UID}-${POD_TEMPLATE}-${MANAGED_SERVER_NAME}-managed-server-pod.yaml
export POD=${DOMAIN_UID}-${MANAGED_SERVER_NAME}
kubectl get cm -n ${DOMAINS_NAMESPACE} ${DOMAIN_UID}-${POD_TEMPLATE}-managed-server-pod-template-cm -o jsonpath='{.data.server-pod\.yaml}' > ${POD_YAML}
sed -i.bak \
  -e "s|%MANAGED_SERVER_NAME%|${MANAGED_SERVER_NAME}|" \
  -e "s|%MANAGED_SERVER_PORT%|${MANAGED_SERVER_PORT}|" \
  -e "s|%ADMIN_SERVER_NAME%|${ADMIN_SERVER_NAME}|" \
  -e "s|%ADMIN_SERVER_PORT%|${ADMIN_SERVER_PORT}|" \
  -e "s|%DOMAIN_NAME%|${DOMAIN_NAME}|" \
  -e "s|%SITCFG_NAME%|${SITCFG}|" \
${POD_YAML}
rm ${POD_YAML}.bak
kubectl apply -f ${POD_YAML}
${THIS_DIR}/wait-for-pod-to-start.sh ${DOMAINS_NAMESPACE} ${POD}

# create a service for the managed server
export SERVICE_YAML=${DOMAINS_NAMESPACE}-${DOMAIN_UID}-${SERVICE_TEMPLATE}-${MANAGED_SERVER_NAME}-managed-server-service.yaml
kubectl get cm -n ${DOMAINS_NAMESPACE} ${DOMAIN_UID}-${SERVICE_TEMPLATE}-managed-server-service-template-cm -o jsonpath='{.data.server-service\.yaml}' > ${SERVICE_YAML}
sed -i.bak \
  -e "s|%MANAGED_SERVER_NAME%|${MANAGED_SERVER_NAME}|" \
  -e "s|%MANAGED_SERVER_PORT%|${MANAGED_SERVER_PORT}|" \
${SERVICE_YAML}
rm ${SERVICE_YAML}.bak
kubectl apply -f ${SERVICE_YAML}
