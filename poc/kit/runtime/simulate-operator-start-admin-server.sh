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

# simulate the operator starting the admin server:

# create a pod for the admin server
export POD_YAML=${DOMAINS_NAMESPACE}-${DOMAIN_UID}-${POD_TEMPLATE}-${ADMIN_SERVER_NAME}-admin-server-pod.yaml
export POD=${DOMAIN_UID}-${ADMIN_SERVER_NAME}
kubectl get cm -n ${DOMAINS_NAMESPACE} ${DOMAIN_UID}-${POD_TEMPLATE}-admin-server-pod-template-cm -o jsonpath='{.data.server-pod\.yaml}' > ${POD_YAML}
sed -i.bak \
  -e "s|%ADMIN_SERVER_NAME%|${ADMIN_SERVER_NAME}|" \
  -e "s|%ADMIN_SERVER_PORT%|${ADMIN_SERVER_PORT}|" \
  -e "s|%DOMAIN_NAME%|${DOMAIN_NAME}|" \
  -e "s|%SITCFG_NAME%|${SITCFG}|" \
${POD_YAML}
rm ${POD_YAML}.bak
kubectl apply -f ${POD_YAML}
${THIS_DIR}/wait-for-pod-to-start.sh ${DOMAINS_NAMESPACE} ${POD}

# create a service for the admin server
export SERVICE_YAML=${DOMAINS_NAMESPACE}-${DOMAIN_UID}-${SERVICE_TEMPLATE}-${ADMIN_SERVER_NAME}-admin-server-service.yaml
kubectl get cm -n ${DOMAINS_NAMESPACE} ${DOMAIN_UID}-${SERVICE_TEMPLATE}-admin-server-service-template-cm -o jsonpath='{.data.server-service\.yaml}' > ${SERVICE_YAML}
sed -i.bak \
  -e "s|%ADMIN_SERVER_NAME%|${ADMIN_SERVER_NAME}|" \
  -e "s|%ADMIN_SERVER_PORT%|${ADMIN_SERVER_PORT}|" \
${SERVICE_YAML}
rm ${SERVICE_YAML}.bak
kubectl apply -f ${SERVICE_YAML}

# create a t3 service for the admin server
export T3_SERVICE_YAML=${DOMAINS_NAMESPACE}-${DOMAIN_UID}-${SERVICE_TEMPLATE}-${ADMIN_SERVER_NAME}-admin-server-t3-service.yaml
kubectl get cm -n ${DOMAINS_NAMESPACE} ${DOMAIN_UID}-${SERVICE_TEMPLATE}-admin-server-t3-service-template-cm -o jsonpath='{.data.server-service\.yaml}' > ${T3_SERVICE_YAML}
sed -i.bak \
  -e "s|%ADMIN_SERVER_NAME%|${ADMIN_SERVER_NAME}|" \
${T3_SERVICE_YAML}
rm ${T3_SERVICE_YAML}.bak
kubectl apply -f ${T3_SERVICE_YAML}

