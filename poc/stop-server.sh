#!/bin/bash

set -x

SERVER_NAME="${1}"

kubectl delete -f ${SERVER_NAME}-pod.yaml
kubectl delete -f ${SERVER_NAME}-service.yaml
