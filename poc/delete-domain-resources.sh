#!/bin/bash

. pocenv.sh
set -x

kubectl delete ns ${DOMAIN_NAMESPACE}
kubectl delete pv poc-duid-weblogic-domain-home-pv
kubectl delete pv poc-duid-weblogic-domain-logs-pv
# tbd - wait for them to be deleted
