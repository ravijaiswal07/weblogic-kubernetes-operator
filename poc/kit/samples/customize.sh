#!/bin/bash

#set -x

fileToCustomize=$1

function customize-property {
  propertyName=${1}
  ${OPERATOR_SAMPLES}/customize-property.sh ${fileToCustomize} ${propertyName}
}

customize-property SETUP_ENV_SCRIPT
customize-property OPERATOR_NAMESPACE
customize-property DOMAIN_NAMESPACE
customize-property DOMAIN_UID
customize-property DOMAIN_NAME
customize-property ADMIN_USERNAME
customize-property ADMIN_PASSWORD
customize-property CLUSTER_NAME
customize-property ADMIN_SERVER_NAME
customize-property ADMIN_SERVER_PORT
customize-property T3_CHANNEL_PORT
customize-property T3_PUBLIC_ADDRESS
customize-property MANAGED_SERVER_BASE_NAME
customize-property MANAGED_SERVER_NAME
customize-property MANAGED_SERVER_PORT
customize-property MANAGED_SERVER_COUNT
customize-property DOMAIN_CREDENTIALS_SECRET_NAME
customize-property PVS_DIR
customize-property DOMAIN_PVS_DIR
customize-property DOMAIN_HOME_PV_DIR
customize-property DOMAIN_LOGS_PV_DIR
customize-property POD_DOMAIN_HOME_DIR
customize-property POD_DOMAIN_LOGS_DIR
