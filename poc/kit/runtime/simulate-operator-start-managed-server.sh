#!/bin/bash

set -x

export THIS_DIR=`dirname ${BASH_SOURCE[0]}`

export OPERATOR_NAMESPACE=$1
export DOMAINS_NAMESPACE=$2
export DOMAIN_UID=$3
export POD_TEMPLATE=$4
export SERVICE_TEMPLATE=$5
export SITCFG=$6
export DOMAIN_NAME=$7
export ADMIN_SERVER_NAME=$8
export ADMIN_SERVER_PORT=$9
export MANAGED_SERVER_NAME=${10}
export MANAGED_SERVER_PORT=${11}
export DESIRED_STATE=${12}

export STARTUP_MODE=""
if [ "ADMIN" == "${DESIRED_STATE}" ]; then
  export STARTUP_MODE=" -Dweblogic.management.startupMode=ADMIN"
fi

# simulate the operator starting a managed server:

# create a pod for the managed server
export POD_YAML=${DOMAIN_UID}-${POD_TEMPLATE}-${MANAGED_SERVER_NAME}-server-pod.yaml
export POD=${DOMAIN_UID}-${MANAGED_SERVER_NAME}
export INTERNAL_OPERATOR_CERT=`kubectl get cm -n ${OPERATOR_NAMESPACE} weblogic-operator-cm -o jsonpath='{.data.internalOperatorCert}'`
kubectl get cm -n ${DOMAINS_NAMESPACE} ${DOMAIN_UID}-${POD_TEMPLATE}-managed-server-pod-template-cm -o jsonpath='{.data.server-pod\.yaml}' > ${POD_YAML}
sed -i.bak \
  -e "s|%SERVER_NAME%|${MANAGED_SERVER_NAME}|" \
  -e "s|%SERVER_PORT%|${MANAGED_SERVER_PORT}|" \
  -e "s|'%SERVER_PORT_AS_INT%'|${MANAGED_SERVER_PORT}|" \
  -e "s|\"%SERVER_PORT_AS_INT%\"|${MANAGED_SERVER_PORT}|" \
  -e "s|%ADMIN_SERVER_NAME%|${ADMIN_SERVER_NAME}|" \
  -e "s|%ADMIN_SERVER_PORT%|${ADMIN_SERVER_PORT}|" \
  -e "s|%DOMAIN_NAME%|${DOMAIN_NAME}|" \
  -e "s|%SITCFG_NAME%|${SITCFG}|" \
  -e "s|%INTERNAL_OPERATOR_CERT%|${INTERNAL_OPERATOR_CERT}|" \
  -e "s|%STARTUP_MODE%|${STARTUP_MODE}|" \
${POD_YAML}
rm ${POD_YAML}.bak
kubectl apply -f ${POD_YAML}
${THIS_DIR}/wait-for-pod-to-start.sh ${DOMAINS_NAMESPACE} ${POD}

# create a service for the managed server
export SERVICE_YAML=${DOMAIN_UID}-${SERVICE_TEMPLATE}-${MANAGED_SERVER_NAME}-server-service.yaml
kubectl get cm -n ${DOMAINS_NAMESPACE} ${DOMAIN_UID}-${SERVICE_TEMPLATE}-managed-server-service-template-cm -o jsonpath='{.data.server-service\.yaml}' > ${SERVICE_YAML}
sed -i.bak \
  -e "s|%SERVER_NAME%|${MANAGED_SERVER_NAME}|" \
  -e "s|%SERVER_PORT%|${MANAGED_SERVER_PORT}|" \
${SERVICE_YAML}
rm ${SERVICE_YAML}.bak
kubectl apply -f ${SERVICE_YAML}
