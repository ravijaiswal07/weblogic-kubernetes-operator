#!/bin/bash

set -x

export THIS_DIR=`dirname ${BASH_SOURCE[0]}`

export DOMAINS_NAMESPACE=$1
export DOMAIN_UID=$2
export SITCFG=$3
export DOMAIN_NAME=$4
export ADMIN_SERVER_NAME=$5
export ADMIN_SERVER_PORT=$6

export POD_YAML=${DOMAINS_NAMESPACE}-${DOMAIN_UID}-admin-server-pod.yaml

# simulate the operator starting the admin server:

kubectl get cm -n ${DOMAINS_NAMESPACE} demo2-domain-uid-admin-server-cm -o jsonpath='{.data.admin-server-pod\.yaml}' > ${POD_YAML}
# customize the template for the admin server
sed -i.bak \
  -e "s|%ADMIN_SERVER_NAME%|${ADMIN_SERVER_NAME}|" \
  -e "s|%ADMIN_SERVER_PORT%|${ADMIN_SERVER_PORT}|" \
  -e "s|%DOMAIN_NAME%|${DOMAIN_NAME}|" \
  -e "s|%SITCFG_NAME%|${SITCFG}|" \
${POD_YAML}
rm ${POD_YAML}.bak

kubectl apply -f ${POD_YAML}
${THIS_DIR}/wait-for-pod-to-start.sh ${DOMAINS_NAMESPACE} ${DOMAIN_UID}-${ADMIN_SERVER_NAME}
