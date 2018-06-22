#!/bin/bash

set -x

export THIS_DIR=`dirname ${BASH_SOURCE[0]}`

export DOMAINS_NAMESPACE=$1
export DOMAIN_UID=$2

# simulate the operator generating the domain topology:

# get the template for creating the pod from the config map and use it to create the pod
kubectl get cm -n ${DOMAINS_NAMESPACE} ${DOMAIN_UID}-domain-introspector-cm -o jsonpath='{.data.domain-introspector-pod\.yaml}' > domain-introspector-pod.yaml

kubectl apply -f domain-introspector-pod.yaml
${THIS_DIR}/wait-for-pod-to-start.sh ${DOMAINS_NAMESPACE} ${DOMAIN_UID}-domain-introspector

# get the introspection results from the pod
kubectl exec -n ${DOMAINS_NAMESPACE} ${DOMAIN_UID}-domain-introspector /weblogic-operator/scripts/getIntrospectionResults.sh

kubectl delete -f domain-introspector-pod.yaml
${THIS_DIR}/wait-for-pod-deleted.sh ${DOMAINS_NAMESPACE} ${DOMAIN_UID}-domain-introspector
rm domain-introspector-pod.yaml