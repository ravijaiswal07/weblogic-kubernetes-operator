#!/bin/bash

set -x

export THIS_DIR=`dirname ${BASH_SOURCE[0]}`

export TEMPLATE=$1
export DOMAINS_NAMESPACE=$2
export DOMAIN_UID=$3

# simulate the operator generating introspecting the domain:

export RESOURCES_YAML=${DOMAIN_UID}-domain-introspector.yaml
export POD=${DOMAIN_UID}-domain-introspector

export TOPOLOGY_YAML=${DOMAIN_UID}-${TEMPLATE}-topology.yaml

# get the template for creating the pod from the config map and use it to create the pod
kubectl get cm -n ${DOMAINS_NAMESPACE} ${DOMAIN_UID}-templates-cm -o jsonpath="{.data.domain-introspector-template-${TEMPLATE}\.yaml}" > ${RESOURCES_YAML}
kubectl apply -f ${RESOURCES_YAML}
${THIS_DIR}/wait-for-pod-to-start.sh ${DOMAINS_NAMESPACE} ${POD}

# get the topology from the pod
kubectl exec -n ${DOMAINS_NAMESPACE} ${POD} /weblogic-operator/scripts/getTopology.sh

# TBD: look at the topology to see whether there the domain was valid
# if it wasn't, print the errors and return
# NOTE: the current wlst script does not generate a situational config or boot.properties
# if the domain is not valid (since it assumes that traversing the domain to generate the
# config may fail if the domain is not valid).
# Ryan wanted the operator to only warn when the domain isn't valid, and try to use it anyway.
# This means that we'd need to do our best to generate boot.properties and the sit cfg even
# when the domain isn't valid.
# Maybe we need two levels of invalid:
#  level1 - fatal - the operator can't work at all
#  level2 - warn - the operator can at least try it

# the pod generated a yaml file that defines a config map that needs to be loaded in to the server pods.
# retrieve it from the pod.
export SERVER_CM_YAML=${DOMAIN_UID}-${TEMPLATE}-server-cm.yaml
kubectl exec -n ${DOMAINS_NAMESPACE} ${POD} /weblogic-operator/scripts/getServerConfigMap.sh > ${SERVER_CM_YAML}

# the pod generated a yaml file that defines a secret that needs to be loaded in to the server pods.
# retrieve it from the pod.
export SERVER_SECRET_YAML=${DOMAIN_UID}-${TEMPLATE}-server-secret.yaml
kubectl exec -n ${DOMAINS_NAMESPACE} ${POD} /weblogic-operator/scripts/getServerSecret.sh > ${SERVER_SECRET_YAML}

# get rid of the pod since we don't need the pod anymore
kubectl delete -f ${RESOURCES_YAML}
${THIS_DIR}/wait-for-pod-deleted.sh ${DOMAINS_NAMESPACE} ${POD}
rm ${RESOURCES_YAML}

# use the yaml to remove the config map that gets mounted into the server pods so that
# they can access its contents
cat ${SERVER_CM_YAML}
kubectl apply -f ${SERVER_CM_YAML}

# use the yaml to remove the secret that gets mounted into the server pods so that they
# can access its contents
cat ${SERVER_SECRET_YAML}
kubectl apply -f ${SERVER_SECRET_YAML}
