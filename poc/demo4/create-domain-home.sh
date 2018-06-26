#!/bin/bash

set -x

export DEMO_HOME=`pwd` # this means that you need to run the sample from this directory!
export DEMO_NAME="demo4"
export OPERATOR_HOME="${DEMO_HOME}/../kit"
export GENERATED_FILES="${DEMO_HOME}/generated"
export OPERATOR_SAMPLES="${OPERATOR_HOME}/samples"
export PVS_DIR="/scratch/k8s-dir"

export WEBLOGIC_VERSION="12.2.1.3.0"
export DOMAIN_UID="${DEMO_NAME}-domain-uid"
export DOMAIN_NAME="${DEMO_NAME}-domain"
export ADMIN_USERNAME="weblogic"
export ADMIN_PASSWORD="welcome1"
export ADMIN_SERVER_NAME="as"
export ADMIN_SERVER_PORT="7100"
export T3_CHANNEL_PORT="30212"
export T3_PUBLIC_ADDRESS="localhost"
export CLUSTER_NAME="cluster"
export MANAGED_SERVER_BASE_NAME="ms"
export MANAGED_SERVER_PORT="8100"
export MANAGED_SERVER_COUNT="3"

export POD_DOMAIN_HOME_DIR="/u01/oracle/domain-home" # TBD - should the customer be able to control this?
export POD_DOMAIN_LOGS_DIR="/domain-logs" # TBD - should the customer be able to control this?

export DOMAIN_PATH="${POD_DOMAIN_HOME_DIR}"
export DOMAIN_PVS_DIR="${PVS_DIR}/${DOMAIN_UID}"
export DOMAIN_HOME_PV_DIR="${DOMAIN_PVS_DIR}${POD_DOMAIN_HOME_DIR}"
export DOMAIN_LOGS_PV_DIR="${DOMAIN_PVS_DIR}${POD_DOMAIN_LOGS_DIR}"

./create-domain-home-with-configured-cluster.sh
