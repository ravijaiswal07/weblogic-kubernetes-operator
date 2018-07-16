#!/bin/bash

set -x

export THIS_DIR=`dirname ${BASH_SOURCE[0]}`

export TEMPLATE=$1
export OPERATOR_NAMESPACE=$2
export DOMAINS_NAMESPACE=$3
export DOMAIN_UID=$4
export DOMAIN_NAME=$5
export ADMIN_SERVER_NAME=$6
export ADMIN_SERVER_PORT=$7
export MANAGED_SERVER_NAME=$8
export MANAGED_SERVER_PORT=$9
export DESIRED_STATE=${10}

export STARTUP_MODE=""
if [ "ADMIN" == "${DESIRED_STATE}" ]; then
  export STARTUP_MODE=" -Dweblogic.management.startupMode=ADMIN"
fi

# simulate the operator starting a managed server:

export RESOURCES_YAML=${DOMAIN_UID}-${MANAGED_SERVER_NAME}.yaml
export POD=${DOMAIN_UID}-${MANAGED_SERVER_NAME}
export INTERNAL_OPERATOR_CERT=`kubectl get cm -n ${OPERATOR_NAMESPACE} weblogic-operator-cm -o jsonpath='{.data.internalOperatorCert}'`
kubectl get cm -n ${DOMAINS_NAMESPACE} ${DOMAIN_UID}-templates-cm -o jsonpath="{.data.managed-server-template-${TEMPLATE}\.yaml}" > ${RESOURCES_YAML}
sed -i.bak \
  -e "s|%ADMIN_SERVER_NAME%|${ADMIN_SERVER_NAME}|" \
  -e "s|%ADMIN_SERVER_PORT%|${ADMIN_SERVER_PORT}|" \
  -e "s|%SERVER_NAME%|${MANAGED_SERVER_NAME}|" \
  -e "s|%SERVER_PORT%|${MANAGED_SERVER_PORT}|" \
  -e "s|'%SERVER_PORT_AS_INT%'|${MANAGED_SERVER_PORT}|" \
  -e "s|\"%SERVER_PORT_AS_INT%\"|${MANAGED_SERVER_PORT}|" \
  -e "s|%DOMAIN_NAME%|${DOMAIN_NAME}|" \
  -e "s|%TEMPLATE_NAME%|${TEMPLATE}|" \
  -e "s|%INTERNAL_OPERATOR_CERT%|${INTERNAL_OPERATOR_CERT}|" \
  -e "s|%STARTUP_MODE%|${STARTUP_MODE}|" \
${RESOURCES_YAML}
rm ${RESOURCES_YAML}.bak
kubectl apply -f ${RESOURCES_YAML}
${THIS_DIR}/wait-for-pod-to-start.sh ${DOMAINS_NAMESPACE} ${POD}
