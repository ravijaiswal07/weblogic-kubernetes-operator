#!/bin/bash

. ./demo-env.sh

export OPERATOR_NAMESPACE="demo-o-ns"
export OPERATOR_SERVICE_ACCOUNT="demo-o-sa"
export OPERATOR_IMAGE="wlsldi-v2.docker.oraclecorp.com/weblogic-operator:tmoreau-lc"
export OPERATOR_IMAGE_PULL_POLICY="Never"
export OPERATOR_JAVA_LOGGING_LEVEL="Info"
export OPERATOR_DOMAINS_NAMESPACES="demo-d-ns"
export OPERATOR_EXTERNAL_REST_HTTPS_PORT=31023

#export OPERATOR_INTERNAL_DEBUG_HTTP_PORT=?
#TBD - operator internal & external REST port certs & private keys as base64 encoded pem
