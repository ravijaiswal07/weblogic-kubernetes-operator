#!/bin/bash

. ./domains-ns-env.sh

set -x

#----------------------------------------------------------------------------------------
# All/most customers need these functions as-is
#----------------------------------------------------------------------------------------

function verifyDomainsNamespaceDoesNotExist {
  # TBD see if the domains namespace exists, and if so, return false
  return true
}

function copyAndCustomize {
  from=${1}
  to=${2}
  cp ${from} ${to}
  ${OPERATOR_SAMPLES}/customize.sh ${to}
}

function createDomainsNamespaceScriptsYaml {
  copyAndCustomize ${OPERATOR_TEMPLATES}/domain-cm.yamlt            ${GENERATED_FILES}/domain-cm.yaml
  copyAndCustomize ${OPERATOR_TEMPLATES}/operator-rolebinding.yamlt ${GENERATED_FILES}/operator-rolebinding.yaml
}

function copyAndCustomizeTemplatesBase {
  createDomainsNamespaceScriptsYaml
}

function createKubernetesResourcesBase {
  # create the kubernetes namespace and domain wide resources for this domain
  # don't create the ones the operator would create at runtime
  kubectl apply -f ${GENERATED_FILES}/domain-cm.yaml
  kubectl apply -f ${GENERATED_FILES}/operator-rolebinding.yaml
}

function createDomainsNamespace {
  kubectl create ns ${DOMAINS_NAMESPACE}
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
  if [ verifyDomainsNamespaceDoesNotExist == false ]; then
    return
  fi
  env
  copyAndCustomizeTemplates
  createDomainsNamespace
  createKubernetesResources
}

main
