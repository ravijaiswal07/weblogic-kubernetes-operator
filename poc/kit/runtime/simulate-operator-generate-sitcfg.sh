#!/bin/bash

set -x

export THIS_DIR=`dirname ${BASH_SOURCE[0]}`

export DOMAINS_NAMESPACE=$1
export DOMAIN_UID=$2
export SITCFG=$3

# simulate the operator generating the situational configuration:

export SITCFG_POD_YAML=${DOMAINS_NAMESPACE}-${DOMAIN_UID}-${SITCFG}-sitcfg-generator-pod.yaml
export SITCFG_POD=${DOMAIN_UID}-${SITCFG}-sitcfg-generator
export SITCFG_CM_YAML=${DOMAINS_NAMESPACE}-${DOMAIN_UID}-${SITCFG}-sitcfg-cm.yaml

kubectl get cm -n ${DOMAINS_NAMESPACE} ${DOMAIN_UID}-${SITCFG}-sitcfg-generator-cm -o jsonpath='{.data.sitcfg-generator-pod\.yaml}' > ${SITCFG_POD_YAML}

kubectl apply -f ${SITCFG_POD_YAML}
${THIS_DIR}/wait-for-pod-to-start.sh ${DOMAINS_NAMESPACE} ${SITCFG_POD}

# the pod generated a yaml file that defines a config map containing the generated sit cfg.
# retrieve it from the pod.
kubectl exec -n ${DOMAINS_NAMESPACE} ${SITCFG_POD} /weblogic-operator/scripts/getGenerateSitCfgResults.sh > ${SITCFG_CM_YAML}
kubectl delete -f ${SITCFG_POD_YAML}
${THIS_DIR}/wait-for-pod-deleted.sh ${DOMAINS_NAMESPACE} ${SITCFG_POD}
rm ${SITCFG_POD_YAML}

# use the yaml to create the config map containing the sit cfg.
# the config map gets mounted into the server pods so that they can access the sit cfg
cat ${SITCFG_CM_YAML}
kubectl apply -f ${SITCFG_CM_YAML}
