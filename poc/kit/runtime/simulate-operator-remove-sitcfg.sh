#!/bin/bash

set -x

export TEMPLATE=$1
export DOMAIN_UID=$2

export SITCFG_CM_YAML=${DOMAIN_UID}-${TEMPLATE}-sitcfg-cm.yaml

# simulate the operator removing the config map that holds the generated situational config
kubectl delete -f ${SITCFG_CM_YAML}
rm ${SITCFG_CM_YAML}
