#!/bin/bash

. ./demoenv.sh

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

function createDomainNamespaceScriptsYaml {
  copyAndCustomize ${OPERATOR_TEMPLATES}/domain-cm.yamlt ${GENERATED_FILES}/domain-cm.yaml
}

function createDomainLogsYamls {
  copyAndCustomize ${OPERATOR_SAMPLES}/domain-logs-pv.yamlt  ${GENERATED_FILES}/domain-logs-pv.yaml
  copyAndCustomize ${OPERATOR_SAMPLES}/domain-logs-pvc.yamlt ${GENERATED_FILES}/domain-logs-pvc.yaml
}

function createAdminServerTemplateYamls {
  copyAndCustomize ${OPERATOR_TEMPLATES}/admin-server-pod.yamlt        ${GENERATED_FILES}/admin-server-pod.yamlt
  copyAndCustomize ${OPERATOR_TEMPLATES}/admin-server-service.yamlt    ${GENERATED_FILES}/admin-server-service.yamlt
  copyAndCustomize ${OPERATOR_TEMPLATES}/admin-server-t3-service.yamlt ${GENERATED_FILES}/admin-server-t3-service.yamlt
}

function createManagedServerTemplateYamls {
  copyAndCustomize ${OPERATOR_TEMPLATES}/managed-server-service.yamlt ${GENERATED_FILES}/managed-server-service.yamlt
  copyAndCustomize ${OPERATOR_TEMPLATES}/managed-server-pod.yamlt     ${GENERATED_FILES}/managed-server-pod.yamlt
}

function createServerScripts {
  copyAndCustomizeServerScript server-log.sh
  copyAndCustomizeServerScript server-nm-log.sh
  copyAndCustomizeServerScript server-nm-state.sh
  copyAndCustomizeServerScript server-out.sh
  copyAndCustomizeServerScript server-pod-desc.sh
  copyAndCustomizeServerScript server-pod-log.sh
  copyAndCustomizeServerScript server-pod-state.sh
  copyAndCustomizeServerScript start-server.sh
  copyAndCustomizeServerScript stop-server.sh
}

function copyAndCustomizeTemplatesBase {
  createDomainNamespaceScriptsYaml
  createDomainLogsYamls
  createAdminServerTemplateYamls
  createManagedServerTemplateYamls
  createServerScripts
}

function createPersistentVolumesBase {
  mkdir -p ${DOMAIN_PVS_DIR}
  mkdir -p ${DOMAIN_LOGS_PV_DIR}
}

function createDomainWideKubernetesResourcesBase {
  # create the kubernetes namespace and domain wide resources for this domain
  # don't create the ones the operator would create at runtime
  kubectl apply -f ${GENERATED_FILES}/domain-cm.yaml
  kubectl apply -f ${GENERATED_FILES}/domain-logs-pv.yaml
  kubectl apply -f ${GENERATED_FILES}/domain-logs-pvc.yaml
}

function simulateOperatorRuntime {
  domain_home_dir=${1}

  # introspect the domain home and create a situational config that fixes the listen addresses
  # and redirects the server and domain logs.  put it in a config map so that the start server
  # script (which runs in the pod) can copy it to the domain home so that the the domain's
  # config gets customized
  # TBD - move this to the server start script in the pod
  ${OPERATOR_RUNTIME}/bind-domain.sh ${domain_home_dir} ${GENERATED_FILES}
  kubectl apply -f ${GENERATED_FILES}/domain-bindings-cm.yaml

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
}

function ensureDomainNamespaceExists {
  kubectl create ns ${DOMAIN_NAMESPACE}
}

function createDomainCredentialsSecret {
  kubectl -n ${DOMAIN_NAMESPACE} create secret generic ${DOMAIN_CREDENTIALS_SECRET_NAME} --from-literal=username=${ADMIN_USERNAME} --from-literal=password=${ADMIN_PASSWORD}
}

#----------------------------------------------------------------------------------------
# Functionality specific to this domain
#----------------------------------------------------------------------------------------

function createDomainHomeYamls {
  copyAndCustomize ${OPERATOR_SAMPLES}/domain-home-pv.yamlt  ${GENERATED_FILES}/domain-home-pv.yaml
  copyAndCustomize ${OPERATOR_SAMPLES}/domain-home-pvc.yamlt ${GENERATED_FILES}/domain-home-pvc.yaml
}

function addDomainHomePersistentVolumeToPodTemplate {
  template=$1
  awk '
  { print }
/    volumeMounts:/ {
    print "    - name: weblogic-domain-home-storage-volume"
    print "      mountPath: %POD_DOMAIN_HOME_DIR%"
}
/  volumes:/ {
    print "  - name: weblogic-domain-home-storage-volume"
    print "    persistentVolumeClaim:"
    print "      claimName: %DOMAIN_UID%-weblogic-domain-home-pvc"
}
' $template > $template.bak
  rm $template
  mv $template.bak $template
  ${OPERATOR_SAMPLES}/customize.sh $template
}

function addDomainHomePersistentVolumeToPods {
  addDomainHomePersistentVolumeToPodTemplate ${GENERATED_FILES}/admin-server-pod.yamlt
  addDomainHomePersistentVolumeToPodTemplate ${GENERATED_FILES}/managed-server-pod.yamlt
}

function copyAndCustomizeTemplates {
  copyAndCustomizeTemplatesBase
  createDomainHomeYamls
  addDomainHomePersistentVolumeToPods
}

function createDomainHome {
  ${OPERATOR_SAMPLES}/create-domain-home-with-configured-cluster.sh
}

function createPersistentVolumes {
  createPersistentVolumesBase
  cp -r ${DOMAIN_PATH} ${DOMAIN_HOME_PV_DIR}
  ${OPERATOR_SAMPLES}/patch-domain-home.sh ${DOMAIN_HOME_PV_DIR}
}

function createDomainWideKubernetesResources {
  createDomainWideKubernetesResourcesBase
  # create the kubernetes namespace and domain wide resources for this domain
  # don't create the ones the operator would create at runtime
  kubectl apply -f ${GENERATED_FILES}/domain-home-pv.yaml
  kubectl apply -f ${GENERATED_FILES}/domain-home-pvc.yaml
}

function main {
  copyAndCustomizeTemplates
  createDomainHome
  createPersistentVolumes
  ensureDomainNamespaceExists
  createDomainCredentialsSecret
  createDomainWideKubernetesResources
  simulateOperatorRuntime ${DOMAIN_PATH}
}

main
