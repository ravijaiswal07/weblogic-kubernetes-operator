#!/bin/bash

set -x

export THIS_DIR=`dirname ${BASH_SOURCE[0]}`

export TEMPLATE=$1
export DOMAINS_NAMESPACE=$2
export DOMAIN_UID=$3

# simulate the operator generating the situational configuration:

export RESOURCES_YAML=${DOMAIN_UID}-sitcfg-generator.yaml
export POD=${DOMAIN_UID}-sitcfg-generator

export SITCFG_CM_YAML=${DOMAIN_UID}-${TEMPLATE}-sitcfg-cm.yaml

kubectl get cm -n ${DOMAINS_NAMESPACE} ${DOMAIN_UID}-templates-cm -o jsonpath="{.data.sitcfg-generator-template-${TEMPLATE}\.yaml}" > ${RESOURCES_YAML}

kubectl apply -f ${RESOURCES_YAML}
${THIS_DIR}/wait-for-pod-to-start.sh ${DOMAINS_NAMESPACE} ${POD}

# the pod generated a yaml file that defines a config map containing the generated sit cfg.
# retrieve it from the pod.
kubectl exec -n ${DOMAINS_NAMESPACE} ${POD} /weblogic-operator/scripts/getGenerateSitCfgResults.sh > ${SITCFG_CM_YAML}
kubectl delete -f ${RESOURCES_YAML}
${THIS_DIR}/wait-for-pod-deleted.sh ${DOMAINS_NAMESPACE} ${POD}
rm ${RESOURCES_YAML}

# use the yaml to create the config map containing the sit cfg.
# the config map gets mounted into the server pods so that they can access the sit cfg
cat ${SITCFG_CM_YAML}
kubectl apply -f ${SITCFG_CM_YAML}
