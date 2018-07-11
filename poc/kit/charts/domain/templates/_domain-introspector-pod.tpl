{{/*
Prints out the extra pod container template properties that the domain introspector pod needs
*/}}
{{- define "domain.domainIntrospectorPodTemplateExtraContainerProperties" -}}
command:
- /weblogic-operator/scripts/introspectDomain.sh
env:
- name: DOMAIN_UID
  value: {{ .domainUid }}
- name: DOMAIN_HOME
  value: {{ .podDomainHomeDir }}
- name: DOMAINS_NAMESPACE
  value: {{ .domainsNamespace }}
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
Creates a pod template that introspects the weblogic domain.
*/}}
{{- define "domain.domainIntrospectorPodTemplate" -}}
{{- $extraContainerProperties := include "domain.domainIntrospectorPodTemplateExtraContainerProperties" . | fromYaml -}}
{{- $args := merge (dict) (omit . "extraContainerProperties") -}}
{{- $ignore := set $args "podName" "domain-introspector" -}}
{{- $ignore := set $args "extraContainerProperties" $extraContainerProperties -}}
{{- include "domain.weblogicPod" $args }}
{{- end }}
