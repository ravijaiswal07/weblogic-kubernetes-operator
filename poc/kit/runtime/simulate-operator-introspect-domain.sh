#!/bin/bash

set -x

export THIS_DIR=`dirname ${BASH_SOURCE[0]}`

export TEMPLATE=$1
export DOMAINS_NAMESPACE=$2
export DOMAIN_UID=$3

# simulate the operator generating the domain topology:

export RESOURCES_YAML=${DOMAIN_UID}-domain-introspector.yaml
export POD=${DOMAIN_UID}-domain-introspector

# get the template for creating the pod from the config map and use it to create the pod
kubectl get cm -n ${DOMAINS_NAMESPACE} ${DOMAIN_UID}-templates-cm -o jsonpath="{.data.domain-introspector-template-${TEMPLATE}\.yaml}" > ${RESOURCES_YAML}

kubectl apply -f ${RESOURCES_YAML}
${THIS_DIR}/wait-for-pod-to-start.sh ${DOMAINS_NAMESPACE} ${POD}

# get the introspection results from the pod
kubectl exec -n ${DOMAINS_NAMESPACE} ${POD} /weblogic-operator/scripts/getIntrospectionResults.sh

kubectl delete -f ${RESOURCES_YAML}
${THIS_DIR}/wait-for-pod-deleted.sh ${DOMAINS_NAMESPACE} ${POD}
rm ${RESOURCES_YAML}
