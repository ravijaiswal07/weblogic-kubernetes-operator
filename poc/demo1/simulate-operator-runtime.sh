#!/bin/bash

. ./domain-env.sh

set -x

#----------------------------------------------------------------------------------------
# All/most customers need these functions as-is
#----------------------------------------------------------------------------------------

function copyAndCustomize {
  from=${1}
  to=${2}
  cp ${from} ${to}
  ${OPERATOR_SAMPLES}/customize.sh ${to}
}

function copyAndCustomizeServerScript {
  script=${1}
  copyAndCustomize ${OPERATOR_SAMPLES}/${1} ${GENERATED_FILES}/${1}
}

#----------------------------------------------------------------------------------------
# Simulate the operator runtime - this is not domain specific
#----------------------------------------------------------------------------------------

function waitForIntrospectionToComplete {
  pod=${DOMAIN_UID}-introspect-domain
  readyHave="false"
  readyWant="true"
  iter=1
  while [ "${readyHave}" != "${readyWant}" -a ${iter} -lt 101 ] ; do
    readyHave=`kubectl get pod -n ${DOMAINS_NAMESPACE} ${pod} -o jsonpath='{.status.containerStatuses[0].ready}'`
    echo readyHave=${readyHave}
    if [ "${readyHave}" != "${readyWant}" ] ; then
      echo "waiting for ${pod} ready, attempt ${iter} : ready=${readyHave}"
      iter=`expr $iter + 1`
      sleep 10
    else
      echo "${pod} is ready"
    fi
  done
  if [ "${readyHave}" != "${readyWant}" ] ; then
    echo "warning: ${pod} still is not ready: ready=${readyHave}"
  fi
}

function downloadIntrospectionResults {
  kubectl exec -n ${DOMAINS_NAMESPACE} ${DOMAIN_UID}-introspect-domain /weblogic-operator/scripts/getIntrospectionResults.sh > ${GENERATED_FILES}/domain-bindings-cm.yaml
}

function applyIntrospectionResults {
  kubectl apply -f ${GENERATED_FILES}/domain-bindings-cm.yaml
}

function introspectDomain {
  kubectl apply -f ${GENERATED_FILES}/introspect-domain-pod.yaml
  waitForIntrospectionToComplete
  downloadIntrospectionResults
  applyIntrospectionResults
}

function simulateOperatorRuntime {
  # create yaml files for introspecting the domain
  copyAndCustomize ${GENERATED_FILES}/introspect-domain-pod.yamlt ${GENERATED_FILES}/introspect-domain-pod.yaml

  # create yaml files for creating the admin server pod and services
  copyAndCustomize ${GENERATED_FILES}/admin-server-pod.yamlt        ${GENERATED_FILES}/${ADMIN_SERVER_NAME}-pod.yaml
  copyAndCustomize ${GENERATED_FILES}/admin-server-service.yamlt    ${GENERATED_FILES}/${ADMIN_SERVER_NAME}-service.yaml
  copyAndCustomize ${GENERATED_FILES}/admin-server-t3-service.yamlt ${GENERATED_FILES}/${ADMIN_SERVER_NAME}-t3-service.yaml

  # create yaml files for creating pods and services for each managed server
  for i in $(seq 1 $MANAGED_SERVER_COUNT); do
    export MANAGED_SERVER_NAME="${MANAGED_SERVER_BASE_NAME}${i}"
    copyAndCustomize ${GENERATED_FILES}/managed-server-pod.yamlt     ${GENERATED_FILES}/${MANAGED_SERVER_NAME}-pod.yaml
    copyAndCustomize ${GENERATED_FILES}/managed-server-service.yamlt ${GENERATED_FILES}/${MANAGED_SERVER_NAME}-service.yaml
  done
  export MANAGED_SERVER_NAME=""

  introspectDomain
}

function main {
  simulateOperatorRuntime
}

main
