#!/bin/bash

. ./domain-env.sh

set -x

#----------------------------------------------------------------------------------------
# All/most customers need these functions as-is
#----------------------------------------------------------------------------------------

function verifyDomainsNamespaceExists {
  # TBD see if the domains namespace exists, and if not, return false
  return true
}

function verifyDomainDoesNotExist {
  # TBD see if the domain exists, and if so, return false
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

function createDomainIntrospectionTemplateYaml {
  copyAndCustomize ${OPERATOR_TEMPLATES}/introspect-domain-pod.yamlt ${GENERATED_FILES}/introspect-domain-pod.yamlt
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
  copyAndCustomizeServerScript wait-for-server-to-start.sh
}

function copyAndCustomizeTemplatesBase {
  createDomainIntrospectionTemplateYaml
  createAdminServerTemplateYamls
  createManagedServerTemplateYamls
  createServerScripts
}

function createDomainCredentialsSecret {
  kubectl -n ${DOMAINS_NAMESPACE} create secret generic ${DOMAIN_CREDENTIALS_SECRET_NAME} --from-literal=username=${ADMIN_USERNAME} --from-literal=password=${ADMIN_PASSWORD}
}

#----------------------------------------------------------------------------------------
# Functionality specific to this domain
#----------------------------------------------------------------------------------------

function createDomainLogsYamls {
  copyAndCustomize ${OPERATOR_SAMPLES}/domain-logs-pv.yamlt  ${GENERATED_FILES}/domain-logs-pv.yaml
  copyAndCustomize ${OPERATOR_SAMPLES}/domain-logs-pvc.yamlt ${GENERATED_FILES}/domain-logs-pvc.yaml
}

function createDomainHomeYamls {
  copyAndCustomize ${OPERATOR_SAMPLES}/domain-home-pv.yamlt  ${GENERATED_FILES}/domain-home-pv.yaml
  copyAndCustomize ${OPERATOR_SAMPLES}/domain-home-pvc.yamlt ${GENERATED_FILES}/domain-home-pvc.yaml
}

function addPersistentVolumeToPodTemplate {
  template=$1
  name=$2
  path=$3
  awk "
  { print }
/    volumeMounts:/ {
    print \"    - name: ${name}-storage-volume\"
    print \"      mountPath: ${path}\"
}
/  volumes:/ {
    print \"  - name: ${name}-storage-volume\"
    print \"    persistentVolumeClaim:\"
    print \"      claimName: ${DOMAIN_UID}-${name}-pvc\"
}
" $template > $template.bak
  rm $template
  mv $template.bak $template
}

function addDomainLogsPersistentVolumeToPodTemplate {
  addPersistentVolumeToPodTemplate ${1} weblogic-domain-logs ${POD_DOMAIN_LOGS_DIR}
}

function addDomainHomePersistentVolumeToPodTemplate {
  addPersistentVolumeToPodTemplate ${1} weblogic-domain-home ${POD_DOMAIN_HOME_DIR}
}

function addDomainLogsPersistentVolumeToPods {
  # note: the introspect pod doesn't need the logs pv since it doesn't write to any logs
  addDomainLogsPersistentVolumeToPodTemplate ${GENERATED_FILES}/admin-server-pod.yamlt
  addDomainLogsPersistentVolumeToPodTemplate ${GENERATED_FILES}/managed-server-pod.yamlt
}

function addDomainHomePersistentVolumeToPods {
  addDomainHomePersistentVolumeToPodTemplate ${GENERATED_FILES}/introspect-domain-pod.yamlt
  addDomainHomePersistentVolumeToPodTemplate ${GENERATED_FILES}/admin-server-pod.yamlt
  addDomainHomePersistentVolumeToPodTemplate ${GENERATED_FILES}/managed-server-pod.yamlt
}

function copyAndCustomizeTemplates {
  copyAndCustomizeTemplatesBase
  createDomainLogsYamls
  createDomainHomeYamls
  addDomainLogsPersistentVolumeToPods
  addDomainHomePersistentVolumeToPods
}

function createDomainHome {
  ${OPERATOR_SAMPLES}/create-domain-home-with-configured-cluster.sh
}

function createDomainLogsPersistentVolume {
  mkdir -p ${DOMAIN_LOGS_PV_DIR}
}

function createDomainHomePersistentVolume {
  cp -r ${DOMAIN_PATH} ${DOMAIN_HOME_PV_DIR}
  ${OPERATOR_SAMPLES}/patch-domain-home.sh ${DOMAIN_HOME_PV_DIR}
}

function createPersistentVolumes {
  mkdir -p ${DOMAIN_PVS_DIR}
  createDomainLogsPersistentVolume
  createDomainHomePersistentVolume
}

function createDomainLogsResources {
  kubectl apply -f ${GENERATED_FILES}/domain-logs-pv.yaml
  kubectl apply -f ${GENERATED_FILES}/domain-logs-pvc.yaml
}

function createDomainHomeResources {
  kubectl apply -f ${GENERATED_FILES}/domain-home-pv.yaml
  kubectl apply -f ${GENERATED_FILES}/domain-home-pvc.yaml
}

function createDomainWideKubernetesResources {
  # create the kubernetes namespace and domain wide resources for this domain
  # don't create the ones the operator would create at runtime
  createDomainLogsResources
  createDomainHomeResources
}

function main {
  if [ verifyDomainsNamespaceExists == false ]; then
    return
  fi
  if [ verifyDomainDoesNotExist == false ]; then
    return
  fi
  copyAndCustomizeTemplates
  createDomainHome
  createPersistentVolumes
  createDomainCredentialsSecret
  createDomainWideKubernetesResources
}

main
