#!/bin/bash

set -x

export TEMPLATE=$1
export DOMAIN_UID=$2

export SERVER_CM_YAML=${DOMAIN_UID}-${TEMPLATE}-server-cm.yaml
export SERVER_SECRET_YAML=${DOMAIN_UID}-${TEMPLATE}-server-secret.yaml

kubectl delete -f ${SERVER_CM_YAML}
rm ${SERVER_CM_YAML}

kubectl delete -f ${SERVER_SECRET_YAML}
rm ${SERVER_SECRET_YAML}
