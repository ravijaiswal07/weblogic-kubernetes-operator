{{/* vim: set filetype=mustache: */}}

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

