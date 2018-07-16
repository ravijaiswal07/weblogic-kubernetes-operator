{{/*
Prints out the extra pod container template properties that the domain introspector pod needs
*/}}
{{- define "domain.domainIntrospectorPodTemplateExtraContainerProperties" -}}
command:
- /weblogic-operator/scripts/introspectDomain.sh
env:
- name: DOMAIN_UID
  value: {{ .domainUID }}
- name: DOMAIN_HOME
  value: {{ .podDomainHomeDir }}
- name: DOMAIN_LOGS
  value: {{ .podDomainLogsDir }}
- name: DOMAINS_NAMESPACE
  value: {{ .domainsNamespace }}
- name: TEMPLATE_NAME
  value: {{ .templateName }}
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
Prints out the extra volume mounts that the domain introspector pod needs
*/}}
{{- define "domain.domainIntrospectorPodTemplateExtraVolumeMounts" -}}
extraVolumeMounts:
- name: weblogic-credentials-volume
  mountPath: /weblogic-operator/secrets
  readOnly: true
{{- if .extraVolumeMounts }}
{{ toYaml .extraVolumeMounts | trim | indent 0 }}
{{- end }}
{{- end }}

{{/*
Prints out the extra volumes that the domain introspector pod needs
*/}}
{{- define "domain.domainIntrospectorPodTemplateExtraVolumes" -}}
extraVolumes:
- name: weblogic-credentials-volume
  secret:
    defaultMode: 420
    secretName:  {{ .weblogicDomainCredentialsSecretName }}
{{- if .extraVolumes }}
{{ toYaml .extraVolumes | trim | indent 0 }}
{{- end }}
{{- end }}

{{/*
Creates a pod template that introspects the weblogic domain.
*/}}
{{- define "domain.domainIntrospectorPodTemplate" -}}
{{- $extraContainerProperties := include "domain.domainIntrospectorPodTemplateExtraContainerProperties" . | fromYaml -}}
{{- $extraVolumeMounts := include "domain.domainIntrospectorPodTemplateExtraVolumeMounts" . | fromYaml -}}
{{- $extraVolumes := include "domain.domainIntrospectorPodTemplateExtraVolumes" . | fromYaml -}}
{{- $args := merge (dict) (omit . "extraContainerProperties" "extraVolumeMounts" "extraVolumes") -}}
{{- $ignore := set $args "podName" "domain-introspector" -}}
{{- $ignore := set $args "extraContainerProperties" $extraContainerProperties -}}
{{- $ignore := set $args "extraVolumeMounts" $extraVolumeMounts.extraVolumeMounts -}}
{{- $ignore := set $args "extraVolumes" $extraVolumes.extraVolumes -}}
{{- include "domain.weblogicPod" $args }}
{{- end }}
