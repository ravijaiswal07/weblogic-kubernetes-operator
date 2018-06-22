#!/bin/bash

set -x

export THIS_DIR=`dirname ${BASH_SOURCE[0]}`

export DOMAINS_NAMESPACE=$1
export DOMAIN_UID=$2
export SITCFG=$3

export SITCFG_CM_YAML=${DOMAINS_NAMESPACE}-${DOMAIN_UID}-${SITCFG}-cm.yaml

# simulate the operator generating the situational configuration:

kubectl get cm -n ${DOMAINS_NAMESPACE} ${DOMAIN_UID}-${SITCFG}-generator-cm -o jsonpath='{.data.sitcfg-generator-pod\.yaml}' > sitcfg-generator-pod.yaml

kubectl apply -f sitcfg-generator-pod.yaml
${THIS_DIR}/wait-for-pod-to-start.sh ${DOMAINS_NAMESPACE} ${DOMAIN_UID}-${SITCFG}-generator

# the pod generated a yaml file that defines a config map containing the generated sit cfg.
# retrieve it from the pod.
kubectl exec -n ${DOMAINS_NAMESPACE} ${DOMAIN_UID}-sitcfg1-generator /weblogic-operator/scripts/getGenerateSitCfgResults.sh > ${SITCFG_CM_YAML}
kubectl delete -f sitcfg-generator-pod.yaml
${THIS_DIR}/wait-for-pod-deleted.sh ${DOMAINS_NAMESPACE} ${DOMAIN_UID}-${SITCFG}-generator
rm sitcfg-generator-pod.yaml

# use the yaml to create the config map containing the sit cfg.
# the config map gets mounted into the server pods so that they can access the sit cfg
cat ${SITCFG_CM_YAML}
kubectl apply -f ${SITCFG_CM_YAML}
