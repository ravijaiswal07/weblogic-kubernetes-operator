{{/* vim: set filetype=mustache: */}}
{{/*
Expand the name of the chart.
*/}}
{{- define "domain.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{/*
Create a default fully qualified app name.
We truncate at 63 chars because some Kubernetes name fields are limited to this (by the DNS naming spec).
If release name contains chart name it will be used as a full name.
*/}}
{{- define "domain.fullname" -}}
{{- if .Values.fullnameOverride -}}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" -}}
{{- else -}}
{{- $name := default .Chart.Name .Values.nameOverride -}}
{{- if contains $name .Release.Name -}}
{{- .Release.Name | trunc 63 | trimSuffix "-" -}}
{{- else -}}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" -}}
{{- end -}}
{{- end -}}
{{- end -}}

{{/*
Create chart name and version as used by the chart label.
*/}}
{{- define "domain.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{/*
Get the values that should be used to create a config map containing
a pod template.
Uses the following scope variables:
  current (required) - contains any specific overrides
  current.podTemplate (optional) - a reference to a custom pod template containing more overrides
  values (required) - the domain wide default values
  values.customPodTemplates (optional) - the domain wide custom pod templates
You can think of it as creating a map containing the values from 'values',
then overlaying the values from 'current.podTemplate' (if it was specified and exists)
then finally overlaying the values from 'current'.
It returns this map as a yaml string.
*/}}
{{- define "domain.getPodTemplateScope" -}}
{{-   $scope := merge dict .current -}}
{{-   $podTemplates := .values.customPodTemplates -}}
{{-   $podTemplateName := .current.podTemplate -}}
{{-   if $podTemplateName -}}
{{-     if $podTemplates -}}
{{-       $podTemplate := index $podTemplates $podTemplateName -}}
{{-       if $podTemplate -}}
{{-         $ignore := merge $scope $podTemplate -}}
{{-       end -}}
{{-     end -}}
{{-   end -}}
{{-   merge $scope .values | toYaml -}}
{{- end -}}

{{/*
The minimum bar for creating a pod that has access to the domain home
Uses the following scope variables:
  operatorNamespace (required)
  domainUID (required)
  domainsNamespace (required)
  weblogicDomainCredentialsSecretName (required)
  configMapName (required)
  podName (required)
  yamlName (required)
  podType (required)
  podName (optional)
  weblogicImage (required)
  weblogicImagePullPolicy (required)
  weblogicimagePullSecret (optional)
  extraPodSpecProperties (optional)
  extraContainerProperties (optional)
  extraLabels (optional)
  extraVolumeMounts (optional)
  extraVolumes (optional)
  extraEnv (optional)
*/}}
{{- define "domain.weblogicPod" -}}
---
kind: ConfigMap
metadata:
  labels:
    weblogic.createdByOperator: "true"
    weblogic.operatorName: {{ .operatorNamespace }}
    weblogic.domainUID: {{ .domainUID }}
    weblogic.resourceVersion: domain-v1
  name: {{ .domainUID }}-{{ .configMapName }}
  namespace: {{ .domainsNamespace }}
data:
  {{ .yamlName }}.yaml: |-
    apiVersion: v1
    kind: Pod
    metadata:
      labels:
        weblogic.createdByOperator: "true"
        weblogic.domainUID: {{ .domainUID }}
        weblogic.resourceVersion: domain-v1
{{- if .extraLabels }}
{{ toYaml .extraLabels | trim | indent 8 }}
{{- end }}
      name: {{ .domainUID }}-{{ .podName }}
      namespace: {{ .domainsNamespace }}
{{- if .extraMetadataProperties }}
{{ toYaml .extraMetadataProperties | trim | indent 6 }}
{{- end }}
    spec:
      containers:
      - image: {{ .weblogicImage }}
        imagePullPolicy: {{ .weblogicImagePullPolicy }}
        name: weblogic-server
        volumeMounts:
        - name: weblogic-credentials-volume
          mountPath: /weblogic-operator/secrets
          readOnly: true
        - name: weblogic-domain-cm-volume
          mountPath: /weblogic-operator/scripts
          readOnly: true
{{- if .extraVolumeMounts }}
{{ toYaml .extraVolumeMounts | trim | indent 8 }}
{{- end }}
{{- if .extraContainerProperties }}
{{ toYaml .extraContainerProperties | trim | indent 8 }}
{{- end }}
      volumes:
      - name: weblogic-credentials-volume
        secret:
          defaultMode: 420
          secretName:  {{ .weblogicDomainCredentialsSecretName }}
      - name: weblogic-domain-cm-volume
        configMap:
          defaultMode: 365
          name: weblogic-domain-cm 
{{- if .extraVolumes }}
{{ toYaml .extraVolumes | trim | indent 6 }}
{{- end }}
{{- if .extraPodSpecProperties }}
{{ toYaml .extraPodSpecProperties | trim | indent 6 }}
{{- end }}
{{- end }}



{{/*
Prints out the extra pod container properties that the domain introspector pod needs
Uses the following scope variables:
  domainUID (required)
  podDomainHomeDir (required)
  podDomainLogsDir (required)
  extraEnv (optional)
  extraContainerProperties (optional)
*/}}
{{- define "domain.domainIntrospectorPodExtraContainerProperties" -}}
command:
- /weblogic-operator/scripts/introspectDomain.sh
env:
- name: DOMAIN_UID
  value: {{ .domainUID }}
- name: DOMAIN_HOME
  value: {{ .podDomainHomeDir }}
{{- if .extraEnv }}
{{ toYaml .extraEnv | trim | indent 0 }}
{{- end }}
livenessProbe:
  exec:
    command:
    - /weblogic-operator/scripts/introspectorLivenessProbe.sh
  failureThreshold: 25
  initialDelaySeconds: 30
  periodSeconds: 10
  successThreshold: 1
  timeoutSeconds: 5
readinessProbe:
  exec:
    command:
    - /weblogic-operator/scripts/introspectorReadinessProbe.sh
  failureThreshold: 1
  initialDelaySeconds: 2
  periodSeconds: 5
  successThreshold: 1
  timeoutSeconds: 5
{{- if .extraContainerProperties }}
{{ toYaml .extraContainerProperties | trim | indent 0 }}
{{- end }}
{{- end }}

{{/*
Creates a pod that introspects the weblogic domain.
Uses the following scope variables:
  introspectorName (required)
  operatorNamespace (required)
  domainUID (required)
  domainsNamespace (required)
  weblogicDomainCredentialsSecretName (required)
  weblogicImage (required)
  weblogicImagePullPolicy (required)
  weblogicimagePullSecret (optional)
  extraPodContainerProperties (optional)
  extraVolumeMounts (optional)
  extraVolumes (optional)
  extraEnv (optional)
  podDomainHomeDir (required)
  podDomainLogsDir (required)
*/}}
{{- define "domain.domainIntrospectorPod" -}}
{{/* compute the domain introspector specific values for this pod */}}
{{- $type := "domain-introspector" -}}
{{- $podName := (join "-" (list .introspectorName $type )) -}}
{{- $configMapName := (join "-" (list $podName "cm" )) -}}
{{- $yamlName := (join "-" (list $type "pod" )) -}}
{{- $extraContainerProperties := include "domain.domainIntrospectorPodExtraContainerProperties" . | fromYaml -}}
{{/* set up the scope needed to create the pod */}}
{{- $args := merge (dict) (omit . "extraContainerProperties") -}}
{{- $ignore := set $args "configMapName" $configMapName -}}
{{- $ignore := set $args "podName" $podName -}}
{{- $ignore := set $args "yamlName" $yamlName -}}
{{- $ignore := set $args "extraContainerProperties" $extraContainerProperties -}}
{{/* create the pod */}}
{{ include "domain.weblogicPod" $args -}}
{{- end }}



{{/*
Prints out the extra pod container properties that a sit cfg generator pod needs
Uses the following scope variables:
  domainUID (required)
  podDomainHomeDir (required)
  extraEnv (optional)
  extraContainerProperties (optional)
*/}}
{{- define "domain.domainSitCfgGeneratorPodExtraContainerProperties" -}}
command:
- /weblogic-operator/scripts/generateSitCfg.sh
env:
- name: DOMAIN_UID
  value: {{ .domainUID }}
- name: DOMAIN_HOME
  value: {{ .podDomainHomeDir }}
- name: DOMAIN_LOGS
  value: {{ .podDomainLogsDir }}
- name: SITCFG_NAME
  value: {{ .sitCfgName }}
{{- if .extraEnv }}
{{ toYaml .extraEnv | trim | indent 0 }}
{{- end }}
livenessProbe:
  exec:
    command:
    - /weblogic-operator/scripts/sitCfgGeneratorLivenessProbe.sh
  failureThreshold: 25
  initialDelaySeconds: 30
  periodSeconds: 10
  successThreshold: 1
  timeoutSeconds: 5
readinessProbe:
  exec:
    command:
    - /weblogic-operator/scripts/sitCfgGeneratorReadinessProbe.sh
  failureThreshold: 1
  initialDelaySeconds: 2
  periodSeconds: 5
  successThreshold: 1
  timeoutSeconds: 5
{{- if .extraContainerProperties }}
{{ toYaml .extraContainerProperties | trim | indent 0 }}
{{- end }}
{{- end }}

{{/*
Creates a pod that generates a situational configuration for a weblogic domain.
Uses the following scope variables:
  sitCfgName (required)
  operatorNamespace (required)
  domainUID (required)
  domainsNamespace (required)
  weblogicDomainCredentialsSecretName (required)
  weblogicImage (required)
  weblogicImagePullPolicy (required)
  weblogicimagePullSecret (optional)
  extraPodContainerProperties (optional)
  extraVolumeMounts (optional)
  extraVolumes (optional)
  extraEnv (optional)
  podDomainHomeDir (required)
*/}}
{{- define "domain.sitCfgGeneratorPod" -}}
{{/* compute the sitcfg generator specific values for this pod */}}
{{- $type := "sitcfg-generator" -}}
{{- $podName := (join "-" (list .sitCfgName $type )) -}}
{{- $configMapName := (join "-" (list $podName "cm" )) -}}
{{- $yamlName := (join "-" (list $type "pod" )) -}}
{{- $extraContainerProperties := include "domain.domainSitCfgGeneratorPodExtraContainerProperties" . | fromYaml -}}
{{/* set up the scope needed to create the pod */}}
{{- $args := merge (dict) (omit . "extraContainerProperties") -}}
{{- $ignore := set $args "configMapName" $configMapName -}}
{{- $ignore := set $args "podName" $podName -}}
{{- $ignore := set $args "yamlName" $yamlName -}}
{{- $ignore := set $args "extraContainerProperties" $extraContainerProperties -}}
{{/* create the pod */}}
{{ include "domain.weblogicPod" $args -}}
{{- end }}


{{/*
Prints out the extra pod container properties that a server pod template needs
Uses the following scope variables:
  domainUID (required)
  podDomainHomeDir (required)
  podDomainLogsDir (required)
  internalOperatorCert (required)
  extraEnv (optional)
  extraContainerProperties (optional)
*/}}
{{- define "domain.serverPodTemplateExtraContainerProperties" -}}
command:
- /weblogic-operator/scripts/startServer.sh
env:
- name: JAVA_OPTIONS
  value: -Dweblogic.StdoutDebugEnabled=false
- name: USER_MEM_ARGS
  value: '-Xms64m -Xmx256m '
- name: INTERNAL_OPERATOR_CERT
  value: {{ .internalOperatorCert }}
- name: DOMAIN_UID
  value: {{ .domainUID }}
- name: DOMAIN_NAME
  value: "%DOMAIN_NAME%"
- name: DOMAIN_HOME
  value: {{ .podDomainHomeDir }}
- name: DOMAIN_LOGS
  value: {{ .podDomainLogsDir }}
- name: ADMIN_NAME
  value: "%ADMIN_SERVER_NAME%"
- name: ADMIN_PORT
  value: "%ADMIN_SERVER_PORT%"
- name: SERVER_NAME
  value: "%SERVER_NAME%"
- name: ADMIN_USERNAME
- name: ADMIN_PASSWORD
{{- if .extraEnv }}
{{ toYaml .extraEnv | trim | indent 0 }}
{{- end }}
lifecycle:
  preStop:
    exec:
      command:
      - /weblogic-operator/scripts/stopServer.sh
livenessProbe:
  exec:
    command:
    - /weblogic-operator/scripts/livenessProbe.sh
  failureThreshold: 1
  initialDelaySeconds: 10
  periodSeconds: 10
  successThreshold: 1
  timeoutSeconds: 5
readinessProbe:
  exec:
    command:
    - /weblogic-operator/scripts/readinessProbe.sh
  failureThreshold: 1
  initialDelaySeconds: 2
  periodSeconds: 5
  successThreshold: 1
  timeoutSeconds: 5
ports:
- containerPort: "%SERVER_PORT_AS_INT%"
  protocol: TCP
{{- if .extraContainerProperties }}
{{ toYaml .extraContainerProperties | trim | indent 0 }}
{{- end }}
{{- end }}

{{/*
Prints out the extra labels that a server pod template needs
*/}}
{{- define "domain.serverPodTemplateExtraLabels" -}}
weblogic.serverName: "%SERVER_NAME%"
{{- end }}

{{/*
Prints out the extra volume mounts that a server pod template needs
*/}}
{{- define "domain.serverPodTemplateExtraVolumeMounts" -}}
extraVolumeMounts:
- name: sitcfg-cm-volume
  mountPath: /weblogic-operator/sitcfg
  readOnly: true
{{- if .domainLogsPersistentVolumeDir }}
- name: weblogic-domain-logs-storage-volume
  mountPath: {{ .podDomainLogsDir }}
{{- end }}
{{- if .extraVolumeMounts }}
{{ toYaml .extraVolumeMounts | trim | indent 0 }}
{{- end }}
{{- end }}

{{/*
Prints out the extra volumes that a server pod template needs
*/}}
{{- define "domain.serverPodTemplateExtraVolumes" -}}
extraVolumes:
- name: sitcfg-cm-volume
  configMap:
    defaultMode: 365
    name: {{ .domainUID }}-%SITCFG_NAME%-sitcfg-cm
{{- if .domainLogsPersistentVolumeDir }}
- name: weblogic-domain-logs-storage-volume
  persistentVolumeClaim:
    claimName: {{ .domainUID }}-weblogic-domain-logs-pvc
{{- end }}
{{- if .extraVolumes }}
{{ toYaml .extraVolumes | trim | indent 0 }}
{{- end }}
{{- end }}

{{/*
Prints out the extra metadata properties that a server pod template needs
*/}}
{{- define "domain.serverPodTemplateExtraMetadataProperties" -}}
annotations:
  prometheus.io/path: /wls-exporter/metrics
  prometheus.io/port: "%SERVER_PORT%"
  prometheus.io/scrape: "true"
{{- end }}

{{/*
Creates a weblogic server pod template.
Uses the following scope variables:
  serverType (required, managed/admin)
  templateName (required)
  operatorNamespace (required)
  domainUID (required)
  domainsNamespace (required)
  weblogicDomainCredentialsSecretName (required)
  weblogicImage (required)
  weblogicImagePullPolicy (required)
  weblogicimagePullSecret (optional)
  extraPodContainerProperties (optional)
  extraVolumeMounts (optional)
  extraVolumes (optional)
  extraEnv (optional)
  podDomainHomeDir (required)
  podDomainLogsDir (required)
*/}}
{{- define "domain.serverPodTemplate" -}}
{{/* compute the server template specific values for this pod */}}
{{- $type := (join "-" (list .serverType "server-pod-template" )) -}}
{{- $podName := "%SERVER_NAME%" -}}
{{- $configMapName := (join "-" (list .templateName $type "cm" )) -}}
{{- $yamlName := "server-pod" -}}
{{- $extraContainerProperties := include "domain.serverPodTemplateExtraContainerProperties" . | fromYaml -}}
{{- $extraMetadataProperties := include "domain.serverPodTemplateExtraMetadataProperties" . | fromYaml -}}
{{- $extraLabels := include "domain.serverPodTemplateExtraLabels" . | fromYaml -}}
{{- $extraVolumeMounts := include "domain.serverPodTemplateExtraVolumeMounts" . | fromYaml -}}
{{- $extraVolumes := include "domain.serverPodTemplateExtraVolumes" . | fromYaml -}}
{{/* set up the scope needed to create the pod */}}
{{- $args := merge (dict) (omit . "extraContainerProperties" "extraVolumeMounts" "extraVolumes" ) -}}
{{- $ignore := set $args "configMapName" $configMapName -}}
{{- $ignore := set $args "podName" $podName -}}
{{- $ignore := set $args "yamlName" $yamlName -}}
{{- $ignore := set $args "extraContainerProperties" $extraContainerProperties -}}
{{- $ignore := set $args "extraMetadataProperties" $extraMetadataProperties -}}
{{- $ignore := set $args "extraLabels" $extraLabels -}}
{{- $ignore := set $args "extraVolumeMounts" $extraVolumeMounts.extraVolumeMounts -}}
{{- $ignore := set $args "extraVolumes" $extraVolumes.extraVolumes -}}
{{/* create the pod */}}
{{ include "domain.weblogicPod" $args -}}
{{- end }}

{{/*
Creates a weblogic managed server pod template.
Uses the following scope variables:
  templateName (required)
  operatorNamespace (required)
  domainUID (required)
  domainsNamespace (required)
  weblogicDomainCredentialsSecretName (required)
  weblogicImage (required)
  weblogicImagePullPolicy (required)
  weblogicimagePullSecret (optional)
  extraPodContainerProperties (optional)
  extraVolumeMounts (optional)
  extraVolumes (optional)
  extraEnv (optional)
  podDomainHomeDir (required)
  podDomainLogsDir (required)
*/}}
{{- define "domain.managedServerPodTemplate" -}}
{{/* compute the server template specific values for this pod */}}
{{- $args := merge (dict) . -}}
{{- $ignore := set $args "serverType" "managed" -}}
{{ include "domain.serverPodTemplate" $args -}}
{{- end }}

{{/*
Prints out the extra pod spec properties that a server pod template needs
Uses the following scope variables:
  domainUID (required)
*/}}
{{- define "domain.adminServerPodTemplateExtraPodSpecProperties" -}}
hostname: {{ .domainUID }}-%SERVER_NAME%
{{- end }}

{{/*
Creates a weblogic admin server pod template.
Uses the following scope variables:
  templateName (required)
  operatorNamespace (required)
  domainUID (required)
  domainsNamespace (required)
  weblogicDomainCredentialsSecretName (required)
  weblogicImage (required)
  weblogicImagePullPolicy (required)
  weblogicimagePullSecret (optional)
  extraPodContainerProperties (optional)
  extraVolumeMounts (optional)
  extraVolumes (optional)
  extraEnv (optional)
  podDomainHomeDir (required)
  podDomainLogsDir (required)
*/}}
{{- define "domain.adminServerPodTemplate" -}}
{{/* compute the server template specific values for this pod */}}
{{- $extraPodSpecProperties := include "domain.adminServerPodTemplateExtraPodSpecProperties" . | fromYaml -}}
{{- $args := merge (dict) . -}}
{{- $ignore := set $args "serverType" "admin" -}}
{{- $ignore := set $args "extraPodSpecProperties" $extraPodSpecProperties -}}
{{ include "domain.serverPodTemplate" $args -}}
{{- end }}
