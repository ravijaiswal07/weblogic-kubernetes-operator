#!/bin/bash

set -x

export THIS_DIR=`dirname ${BASH_SOURCE[0]}`

export DOMAINS_NAMESPACE=$1
export DOMAIN_UID=$2
export INTROSPECTOR=$3

# simulate the operator generating the domain topology:

export INTROSPECTOR_POD_YAML=${DOMAIN_UID}-${SITCFG}-sitcfg-generator-cm.yaml
export INTROSPECTOR_POD=${DOMAIN_UID}-${INTROSPECTOR}-domain-introspector

# get the template for creating the pod from the config map and use it to create the pod
kubectl get cm -n ${DOMAINS_NAMESPACE} ${DOMAIN_UID}-${INTROSPECTOR}-domain-introspector-cm -o jsonpath='{.data.domain-introspector-pod\.yaml}' > ${INTROSPECTOR_POD_YAML}

kubectl apply -f ${INTROSPECTOR_POD_YAML}
${THIS_DIR}/wait-for-pod-to-start.sh ${DOMAINS_NAMESPACE} ${INTROSPECTOR_POD}

# get the introspection results from the pod
kubectl exec -n ${DOMAINS_NAMESPACE} ${INTROSPECTOR_POD} /weblogic-operator/scripts/getIntrospectionResults.sh

kubectl delete -f ${INTROSPECTOR_POD_YAML}
${THIS_DIR}/wait-for-pod-deleted.sh ${DOMAINS_NAMESPACE} ${INTROSPECTOR_POD}
rm ${INTROSPECTOR_POD_YAML}
