#!/bin/bash

export OPERATOR_NAMESPACE='poc-o-ns'
export DOMAIN_NAMESPACE='poc-d-ns'
export DOMAIN_UID='poc-duid'
export DOMAIN_NAME='poc-domain'
export ADMIN_USERNAME='weblogic'
export ADMIN_PASSWORD='welcome1'
export CLUSTER_NAME='cluster'
export ADMIN_SERVER_NAME='as'
export MANAGED_SERVER_NAME_BASE='ms'
export ADMIN_SERVER_PORT='7001'
export MANAGED_SERVER_PORT='8001'
export T3_CHANNEL_PORT='30212'
export T3_PUBLIC_ADDRESS='localhost'
export MANAGED_SERVER_COUNT='3'

export DOMAIN_CREDENTIALS_SECRET_NAME="poc-d-creds"
export PVS_DIR='/scratch/k8s-dir'
export DOMAIN_PVS_DIR="${PVS_DIR}/${DOMAIN_UID}"
export DOMAIN_HOME_PV_DIR="${DOMAIN_PVS_DIR}/domain-home"
export DOMAIN_LOGS_PV_DIR="${DOMAIN_PVS_DIR}/domain-logs"
export POD_DOMAIN_HOME_DIR="/domain-home"
export POD_DOMAIN_LOGS_DIR="/domain-logs"

# /shared/domain/<domainName> -> /domain-home
# /shared/logs -> /domain-logs
