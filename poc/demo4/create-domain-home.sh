#!/bin/bash

set -x

export DEMO_HOME=`pwd` # this means that you need to run the sample from this directory!
export DEMO_NAME="demo4"

export WEBLOGIC_VERSION="12.2.1.3.0"
export DOMAIN_UID="${DEMO_NAME}-domain-uid"
export DOMAIN_NAME="${DEMO_NAME}-domain"
export ADMIN_USERNAME="weblogic"
export ADMIN_PASSWORD="welcome1"
export ADMIN_SERVER_NAME="as"
export ADMIN_SERVER_PORT="7400"
export T3_CHANNEL_PORT="30412"
export T3_PUBLIC_ADDRESS="localhost"
export CLUSTER_NAME="cluster"
export MANAGED_SERVER_BASE_NAME="ms"
export MANAGED_SERVER_PORT="8400"
export MANAGED_SERVER_COUNT="3"

export POD_DOMAIN_HOME_DIR="/u01/oracle/domain-home" # TBD - should the customer be able to control this?
export POD_DOMAIN_LOGS_DIR="/domain-logs" # TBD - should the customer be able to control this?

export DOMAIN_PATH="${POD_DOMAIN_HOME_DIR}"

./create-domain-home-with-configured-cluster.sh
