#!/bin/bash

#set -x

fileToCustomize=$1

function customize-property {
  propertyName=${1}
  ${OPERATOR_SAMPLES}/customize-property.sh ${fileToCustomize} ${propertyName}
}

# demo level
customize-property PVS_DIR

# operator level:
customize-property OPERATOR_SERVICE_ACCOUNT
customize-property OPERATOR_NAMESPACE
customize-property OPERATOR_DOMAINS_NAMESPACES
customize-property OPERATOR_IMAGE
customize-property OPERATOR_IMAGE_PULL_POLICY
customize-property OPERATOR_JAVA_LOGGING_LEVEL
customize-property OPERATOR_EXTERNAL_REST_HTTPS_PORT
customize-property OPERATOR_INTERNAL_DEBUG_HTTP_PORT
customize-property OPERATOR_EXTERNAL_DEBUG_HTTP_PORT

# domains ns level:
customize-property DOMAINS_NAMESPACE

customize-property DOMAIN_NAME

# domain level:
customize-property DOMAIN_UID
customize-property WEBLOGIC_VERSION
customize-property WEBLOGIC_IMAGE
customize-property WEBLOGIC_IMAGE_PULL_POLICY
customize-property SETUP_SCRIPT_ENV_SCRIPT
customize-property DOMAIN_CREDENTIALS_SECRET_NAME
customize-property DOMAIN_PVS_DIR
customize-property DOMAIN_HOME_PV_DIR
customize-property DOMAIN_LOGS_PV_DIR
customize-property POD_DOMAIN_HOME_DIR
customize-property POD_DOMAIN_LOGS_DIR

# create domain home level:
# (some of the yaml resource templates and scripts might be temporarily referring to them v.s. relying on domain introspection)
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
