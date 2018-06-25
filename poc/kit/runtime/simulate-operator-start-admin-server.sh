#!/bin/bash

set -x

export THIS_DIR=`dirname ${BASH_SOURCE[0]}`

export DOMAINS_NAMESPACE=$1
export DOMAIN_UID=$2
export ADMIN_TEMPLATE=$3
export SITCFG=$4
export DOMAIN_NAME=$5
export ADMIN_SERVER_NAME=$6
export ADMIN_SERVER_PORT=$7

# simulate the operator starting the admin server:

export ADMIN_POD_YAML=${DOMAINS_NAMESPACE}-${DOMAIN_UID}-${ADMIN_TEMPLATE}-${ADMIN_SERVER_NAME}-admin-server-pod.yaml
export ADMIN_POD=${DOMAIN_UID}-${ADMIN_SERVER_NAME}

kubectl get cm -n ${DOMAINS_NAMESPACE} ${DOMAIN_UID}-${ADMIN_TEMPLATE}-admin-server-template-cm -o jsonpath='{.data.admin-server-pod\.yaml}' > ${ADMIN_POD_YAML}
# customize the template for the admin server
sed -i.bak \
  -e "s|%ADMIN_SERVER_NAME%|${ADMIN_SERVER_NAME}|" \
  -e "s|%ADMIN_SERVER_PORT%|${ADMIN_SERVER_PORT}|" \
  -e "s|%DOMAIN_NAME%|${DOMAIN_NAME}|" \
  -e "s|%SITCFG_NAME%|${SITCFG}|" \
${ADMIN_POD_YAML}
rm ${ADMIN_POD_YAML}.bak

kubectl apply -f ${ADMIN_POD_YAML}
${THIS_DIR}/wait-for-pod-to-start.sh ${DOMAINS_NAMESPACE} ${ADMIN_POD}

