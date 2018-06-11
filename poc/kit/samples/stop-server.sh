#!/bin/bash

. %SETUP_ENV_SCRIPT%

#set -x

SERVER_NAME="${1}"

kubectl delete -f ${GENERATED_FILES}/${SERVER_NAME}-pod.yaml
kubectl delete -f ${GENERATED_FILES}/${SERVER_NAME}-service.yaml
