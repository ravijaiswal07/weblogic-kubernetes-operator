#!/bin/bash

. ./operator-env.sh

set -x

#----------------------------------------------------------------------------------------
# All/most customers need these functions as-is
#----------------------------------------------------------------------------------------

function verifyOperatorNamespaceDoesNotExist {
  # TBD see if the operator namespace exists, and if so, return false
  return true
}

function copyAndCustomize {
  from=${1}
  to=${2}
  cp ${from} ${to}
  ${OPERATOR_SAMPLES}/customize.sh ${to}
}

function createDomainNamespaceScriptsYaml {
  copyAndCustomize ${OPERATOR_TEMPLATES}/operator-clusterrolebinding.yamlt                ${GENERATED_FILES}/operator-clusterrolebinding.yaml
  copyAndCustomize ${OPERATOR_TEMPLATES}/operator-clusterrolebinding-auth-delegator.yamlt ${GENERATED_FILES}/operator-clusterrolebinding-auth-delegator.yaml
  copyAndCustomize ${OPERATOR_TEMPLATES}/operator-clusterrolebinding-discovery.yamlt      ${GENERATED_FILES}/operator-clusterrolebinding-discovery.yaml
  copyAndCustomize ${OPERATOR_TEMPLATES}/operator-clusterrolebinding-nonresource.yamlt    ${GENERATED_FILES}/operator-clusterrolebinding-nonresource.yaml
  copyAndCustomize ${OPERATOR_TEMPLATES}/operator-sa.yamlt                                ${GENERATED_FILES}/operator-sa.yaml
  copyAndCustomize ${OPERATOR_TEMPLATES}/operator-cm.yamlt                                ${GENERATED_FILES}/operator-cm.yaml
  copyAndCustomize ${OPERATOR_TEMPLATES}/operator-secrets.yamlt                           ${GENERATED_FILES}/operator-secrets.yaml
  copyAndCustomize ${OPERATOR_TEMPLATES}/operator-dep.yamlt                               ${GENERATED_FILES}/operator-dep.yaml
  copyAndCustomize ${OPERATOR_TEMPLATES}/operator-internal-svc.yamlt                      ${GENERATED_FILES}/operator-internal-svc.yaml
  copyAndCustomize ${OPERATOR_TEMPLATES}/operator-external-svc.yamlt                      ${GENERATED_FILES}/operator-external-svc.yaml
}

function copyAndCustomizeTemplatesBase {
  createDomainNamespaceScriptsYaml
}

function createKubernetesResourcesBase {
  kubectl apply -f ${GENERATED_FILES}/operator-clusterrolebinding.yaml
  kubectl apply -f ${GENERATED_FILES}/operator-clusterrolebinding-auth-delegator.yaml
  kubectl apply -f ${GENERATED_FILES}/operator-clusterrolebinding-discovery.yaml
  kubectl apply -f ${GENERATED_FILES}/operator-clusterrolebinding-nonresource.yaml
  kubectl apply -f ${GENERATED_FILES}/operator-sa.yaml
  kubectl apply -f ${GENERATED_FILES}/operator-cm.yaml
  kubectl apply -f ${GENERATED_FILES}/operator-secrets.yaml
  kubectl apply -f ${GENERATED_FILES}/operator-dep.yaml
  kubectl apply -f ${GENERATED_FILES}/operator-internal-svc.yaml
  kubectl apply -f ${GENERATED_FILES}/operator-external-svc.yaml
}

function createOperatorNamespace {
  kubectl create ns ${OPERATOR_NAMESPACE}
}

#----------------------------------------------------------------------------------------
# Functionality specific to this domains namespace
#----------------------------------------------------------------------------------------

function enableELK {
  file=${GENERATED_FILES}/operator-dep.yaml
  awk "
  { print }
/      containers:/ {
    print \"      - name: logstash\"
    print \"        image: logstash:5\"
    print \"        args: ['-f', '/logs/logstash.conf']\"
    print \"        volumeMounts:\"
    print \"        - mountPath: /logs\"
    print \"          name: log-dir\"
    print \"        env:\"
    print \"        - name: ELASTICSEARCH_HOST\"
    print \"          value: elasticsearch.default.svc.cluster.local\"
    print \"        - name: ELASTICSEARCH_PORT\"
    print \"          value: '9200'\"
}
/        volumeMounts:/ {
    print \"        - mountPath: /logs\"
    print \"          name: log-dir\"
    print \"          readOnly: false\"
}
/      volumes:/ {
    print \"      - name: log-dir\"
    print \"        emptyDir:\"
    print \"          medium: Memory\"
}
" $file > $file.bak
  rm $file
  mv $file.bak $file
}

function copyAndCustomizeTemplates {
  copyAndCustomizeTemplatesBase
  enableELK
}

function createKubernetesResources {
  createKubernetesResourcesBase
}

function main {
  if [ verifyOperatorNamespaceDoesNotExist == false ]; then
    return
  fi
  copyAndCustomizeTemplates
  createOperatorNamespace
  createKubernetesResources
}

main
