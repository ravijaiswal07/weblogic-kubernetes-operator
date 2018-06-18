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
  copyAndCustomize ${OPERATOR_TEMPLATES}/operator-rolebinding.yamlt                ${GENERATED_FILES}/operator-rolebinding.yaml
  copyAndCustomize ${OPERATOR_TEMPLATES}/operator-rolebinding-auth-delegator.yamlt ${GENERATED_FILES}/operator-rolebinding-auth-delegator.yaml
  copyAndCustomize ${OPERATOR_TEMPLATES}/operator-rolebinding-discovery.yamlt      ${GENERATED_FILES}/operator-rolebinding-discovery.yaml
  copyAndCustomize ${OPERATOR_TEMPLATES}/operator-rolebinding-nonresource.yamlt    ${GENERATED_FILES}/operator-rolebinding-nonresource.yaml
  copyAndCustomize ${OPERATOR_TEMPLATES}/operator-sa.yamlt                         ${GENERATED_FILES}/operator-sa.yaml
  copyAndCustomize ${OPERATOR_TEMPLATES}/operator-cm.yamlt                         ${GENERATED_FILES}/operator-cm.yaml
  copyAndCustomize ${OPERATOR_TEMPLATES}/operator-secrets.yamlt                    ${GENERATED_FILES}/operator-secrets.yaml
  copyAndCustomize ${OPERATOR_TEMPLATES}/operator-dep.yamlt                        ${GENERATED_FILES}/operator-dep.yaml
  copyAndCustomize ${OPERATOR_TEMPLATES}/operator-internal-svc.yamlt               ${GENERATED_FILES}/operator-internal-svc.yaml
  copyAndCustomize ${OPERATOR_TEMPLATES}/operator-external-svc.yamlt               ${GENERATED_FILES}/operator-external-svc.yaml
}

function copyAndCustomizeTemplatesBase {
  createDomainNamespaceScriptsYaml
}

function createKubernetesResourcesBase {
  kubectl apply -f ${GENERATED_FILES}/operator-rolebinding.yaml
  kubectl apply -f ${GENERATED_FILES}/operator-rolebinding-auth-delegator.yaml
  kubectl apply -f ${GENERATED_FILES}/operator-rolebinding-discovery.yaml
  kubectl apply -f ${GENERATED_FILES}/operator-rolebinding-nonresource.yaml
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

function copyAndCustomizeTemplates {
  copyAndCustomizeTemplatesBase
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
