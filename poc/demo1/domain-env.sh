#!/bin/bash

. ./domains-ns-env.sh

export DOMAIN_UID="${DEMO_NAME}-domain-uid"
export WEBLOGIC_IMAGE="store/oracle/weblogic:12.2.1.3"
export WEBLOGIC_IMAGE_PULL_POLICY="IfNotPresent"
export DOMAIN_NAME="${DEMO_NAME}-domain"
export ADMIN_USERNAME="weblogic"
export ADMIN_PASSWORD="welcome1"
export CLUSTER_NAME="cluster"
export ADMIN_SERVER_NAME="as"
export ADMIN_SERVER_PORT="7100"
export T3_CHANNEL_PORT="30212"
export T3_PUBLIC_ADDRESS="localhost"
export MANAGED_SERVER_BASE_NAME="ms"
export MANAGED_SERVER_PORT="8100"
export MANAGED_SERVER_COUNT="3"
export DOMAIN_CREDENTIALS_SECRET_NAME="${DOMAIN_UID}-domain-creds"
export POD_DOMAIN_HOME_DIR="/domain-home" # TBD - should the customer be able to control this?
export POD_DOMAIN_LOGS_DIR="/domain-logs" # TBD - should the customer be able to control this?

export DOMAIN_PATH="${GENERATED_FILES}${POD_DOMAIN_HOME_DIR}"
export DOMAIN_PVS_DIR="${PVS_DIR}/${DOMAIN_UID}"
export DOMAIN_HOME_PV_DIR="${DOMAIN_PVS_DIR}${POD_DOMAIN_HOME_DIR}"
export DOMAIN_LOGS_PV_DIR="${DOMAIN_PVS_DIR}${POD_DOMAIN_LOGS_DIR}"
