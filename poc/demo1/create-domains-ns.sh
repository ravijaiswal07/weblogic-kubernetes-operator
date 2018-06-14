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

function copyAndCustomizeServerScript {
  script=${1}
  copyAndCustomize ${OPERATOR_SAMPLES}/${1} ${GENERATED_FILES}/${1}
}

function createDomainNamespaceScriptsYaml {
  copyAndCustomize ${OPERATOR_TEMPLATES}/domain-cm.yamlt ${GENERATED_FILES}/domain-cm.yaml
}

function copyAndCustomizeTemplatesBase {
  createDomainNamespaceScriptsYaml
}

function createKubernetesResourcesBase {
  # create the kubernetes namespace and domain wide resources for this domain
  # don't create the ones the operator would create at runtime
  kubectl apply -f ${GENERATED_FILES}/domain-cm.yaml
}

function createDomainNamespace {
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
  copyAndCustomizeTemplates
  createDomainNamespace
  createKubernetesResources
}

main
