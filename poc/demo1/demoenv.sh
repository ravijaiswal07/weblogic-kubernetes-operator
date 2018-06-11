#!/bin/bash

export DEMO_HOME=`pwd` # this means that you need to run the sample from this directory!
export SETUP_ENV_SCRIPT="${DEMO_HOME}/demoenv.sh"

export OPERATOR_HOME="${DEMO_HOME}/../kit"
export OPERATOR_SAMPLES="${OPERATOR_HOME}/samples"
export OPERATOR_TEMPLATES="${OPERATOR_HOME}/templates"
export OPERATOR_RUNTIME="${OPERATOR_HOME}/runtime"
export GENERATED_FILES="${DEMO_HOME}/generated"
export PVS_DIR="/scratch/k8s-dir"

export DEMO_NAME="demo1"

export OPERATOR_NAMESPACE="demo-o-ns"
export DOMAIN_NAMESPACE="demo-d-ns"
export DOMAIN_UID="${DEMO_NAME}-domain-uid"
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
export DOMAIN_CREDENTIALS_SECRET_NAME="${DEMO_NAME}-domain-creds"
export POD_DOMAIN_HOME_DIR="/domain-home" # TBD - should the customer be able to control this?
export POD_DOMAIN_LOGS_DIR="/domain-logs" # TBD - should the customer be able to control this?

export DOMAIN_PATH="${GENERATED_FILES}${POD_DOMAIN_HOME_DIR}"
export DOMAIN_PVS_DIR="${PVS_DIR}/${DOMAIN_UID}"
export DOMAIN_HOME_PV_DIR="${DOMAIN_PVS_DIR}${POD_DOMAIN_HOME_DIR}"
export DOMAIN_LOGS_PV_DIR="${DOMAIN_PVS_DIR}${POD_DOMAIN_LOGS_DIR}"
