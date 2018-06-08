#!/bin/bash

. pocenv.sh
set -x

# TBD - run delete-domain-resources.sh ?

kubectl create ns ${DOMAIN_NAMESPACE}
kubectl -n ${DOMAIN_NAMESPACE} create secret generic ${DOMAIN_CREDENTIALS_SECRET_NAME} --from-literal=username=${ADMIN_USERNAME} --from-literal=password=${ADMIN_PASSWORD}

kubectl apply -f domain-cm.yaml
kubectl apply -f domain-home-pv.yaml
kubectl apply -f domain-home-pvc.yaml
kubectl apply -f domain-logs-pv.yaml
kubectl apply -f domain-logs-pvc.yaml
kubectl apply -f domain-bindings-cm.yaml
# TBD - wait for them to be created
