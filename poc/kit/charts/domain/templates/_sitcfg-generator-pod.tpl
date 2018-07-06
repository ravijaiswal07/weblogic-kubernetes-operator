{{/* vim: set filetype=mustache: */}}

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
- name: DOMAINS_NAMESPACE
  value: {{ .domainsNamespace }}
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
