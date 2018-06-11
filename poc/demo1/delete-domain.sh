#!/bin/bash

. ./demoenv.sh

set -x

function deleteDomainNamespace {
  kubectl delete ns ${DOMAIN_NAMESPACE}
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
}

function main {
  deleteDomainNamespace
    # TBD - should kubectl delete -f ... instead since the namespace can be shared by other domains
    # Ditto for this domain's secret
  kubectl delete pv ${DOMAIN_UID}-weblogic-domain-home-pv
  kubectl delete pv ${DOMAIN_UID}-weblogic-domain-logs-pv
  rm -rf ${PVS_DIR}
  rm -r ${GENERATED_FILES}
  mkdir -p ${GENERATED_FILES}
}

main
