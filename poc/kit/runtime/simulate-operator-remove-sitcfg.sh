#!/bin/bash

set -x

export DOMAINS_NAMESPACE=$1
export DOMAIN_UID=$2
export SITCFG=$3

export SITCFG_CM_YAML=${DOMAINS_NAMESPACE}-${DOMAIN_UID}-${SITCFG}-cm.yaml

# simulate the operator removing the config map that holds the generated situational config
kubectl delete -f ${SITCFG_CM_YAML}
rm ${SITCFG_CM_YAML}
