#!/bin/bash

set -x

export THIS_DIR=`dirname ${BASH_SOURCE[0]}`

export DOMAINS_NAMESPACE=$1
export DOMAIN_UID=$2
export MANAGED_TEMPLATE=$3
export SITCFG=$4
export DOMAIN_NAME=$5
export ADMIN_SERVER_NAME=$6
export ADMIN_SERVER_PORT=$7
export MANAGED_SERVER_NAME=$8
export MANAGED_SERVER_PORT=$9

# simulate the operator starting the admin server:

export MANAGED_POD_YAML=${DOMAINS_NAMESPACE}-${DOMAIN_UID}-${MANAGED_TEMPLATE}-${MANAGED_SERVER_NAME}-managed-server-pod.yaml
export MANAGED_POD=${DOMAIN_UID}-${MANAGED_SERVER_NAME}

kubectl get cm -n ${DOMAINS_NAMESPACE} ${DOMAIN_UID}-${MANAGED_TEMPLATE}-managed-server-template-cm -o jsonpath='{.data.managed-server-pod\.yaml}' > ${MANAGED_POD_YAML}
# customize the template for the admin server
sed -i.bak \
  -e "s|%MANAGED_SERVER_NAME%|${MANAGED_SERVER_NAME}|" \
  -e "s|%MANAGED_SERVER_PORT%|${MANAGED_SERVER_PORT}|" \
  -e "s|%ADMIN_SERVER_NAME%|${ADMIN_SERVER_NAME}|" \
  -e "s|%ADMIN_SERVER_PORT%|${ADMIN_SERVER_PORT}|" \
  -e "s|%DOMAIN_NAME%|${DOMAIN_NAME}|" \
  -e "s|%SITCFG_NAME%|${SITCFG}|" \
${MANAGED_POD_YAML}
rm ${MANAGED_POD_YAML}.bak

kubectl apply -f ${MANAGED_POD_YAML}
${THIS_DIR}/wait-for-pod-to-start.sh ${DOMAINS_NAMESPACE} ${MANAGED_POD}
