#!/bin/bash

. ./demo-env.sh

set -x

#----------------------------------------------------------------------------------------
# All/most customers need these functions as-is
#----------------------------------------------------------------------------------------

function copyAndCustomizeTemplatesBase {
  cp ${OPERATOR_TEMPLATES}/operator-clusterrole.yaml             ${GENERATED_FILES}/operator-clusterrole.yaml
  cp ${OPERATOR_TEMPLATES}/operator-clusterrole-nonresource.yaml ${GENERATED_FILES}/operator-clusterrole-nonresource.yaml
  cp ${OPERATOR_TEMPLATES}/operator-clusterrole-namespace.yaml   ${GENERATED_FILES}/operator-clusterrole-namespace.yaml
}

function createKubernetesResourcesBase {
  kubectl apply -f ${GENERATED_FILES}/operator-clusterrole.yaml
  kubectl apply -f ${GENERATED_FILES}/operator-clusterrole-nonresource.yaml
  kubectl apply -f ${GENERATED_FILES}/operator-clusterrole-namespace.yaml
}

#----------------------------------------------------------------------------------------
# Functionality specific to this domains namespace
#----------------------------------------------------------------------------------------

function copyAndCustomizeELKIntegrationTemplates {
  cp ${OPERATOR_TEMPLATES}/kibana-dep.yaml        ${GENERATED_FILES}/kibana-dep.yaml
  cp ${OPERATOR_TEMPLATES}/kibana-svc.yaml        ${GENERATED_FILES}/kibana-svc.yaml
  cp ${OPERATOR_TEMPLATES}/elasticsearch-dep.yaml ${GENERATED_FILES}/elasticsearch-dep.yaml
  cp ${OPERATOR_TEMPLATES}/elasticsearch-svc.yaml ${GENERATED_FILES}/elasticsearch-svc.yaml
}

function copyAndCustomizeTemplates {
  copyAndCustomizeTemplatesBase
  copyAndCustomizeELKIntegrationTemplates
}

function createELKIntegrationKubernetesResources {
  kubectl apply -f ${GENERATED_FILES}/kibana-dep.yaml
  kubectl apply -f ${GENERATED_FILES}/kibana-svc.yaml
  kubectl apply -f ${GENERATED_FILES}/elasticsearch-dep.yaml
  kubectl apply -f ${GENERATED_FILES}/elasticsearch-svc.yaml
}

function createKubernetesResources {
  createKubernetesResourcesBase
  createELKIntegrationKubernetesResources
}

function main {
  copyAndCustomizeTemplates
  createKubernetesResources
}

main
