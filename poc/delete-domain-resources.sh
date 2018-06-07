#!/bin/bash

. pocenv.sh
set -x

kubectl delete ns ${DOMAIN_NAMESPACE}
kubectl delete pv poc-duid-weblogic-domain-home-pv
kubectl delete pv poc-duid-weblogic-domain-logs-pv

set +x
ns_count=-1
iter=1
while [ $ns_count != 0 -a $iter -lt 101 ] ; do
  ns_count=`kubectl get ns -n ${DOMAIN_NAMESPACE} | grep ${DOMAIN_NAMESPACE} | wc -l | xargs`
  if [ $ns_count != 0 ] ; then
    echo "waiting for ${DOMAIN_NAMESPACE} to be deleted, attempt ${iter} : ${ns_count}"
    iter=`expr $iter + 1`
    sleep 10
  else
    echo "${DOMAIN_NAMESPACE} has been successfully deleted"
  fi
done
if [ $ns_count != 0 ] ; then
  echo "warning: ${DOMAIN_NAMEPACE} still exists: ${ns_count}"
fi
