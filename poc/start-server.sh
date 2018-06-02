#!/bin/bash

set -x

SERVER_NAME="${1}"

kubectl apply -f ${SERVER_NAME}-pod.yaml
kubectl apply -f ${SERVER_NAME}-service.yaml
