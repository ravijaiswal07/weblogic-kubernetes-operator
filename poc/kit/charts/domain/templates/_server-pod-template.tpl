{{/*
Prints out the extra pod container properties that a server pod template needs
*/}}
{{- define "domain.serverPodTemplateExtraContainerProperties" -}}
command:
- /weblogic-operator/scripts/startServer.sh
env:
- name: JAVA_OPTIONS
  value: "-Dweblogic.StdoutDebugEnabled=false%STARTUP_MODE%"
- name: USER_MEM_ARGS
  value: '-Xms64m -Xmx256m '
- name: INTERNAL_OPERATOR_CERT
  value: "%INTERNAL_OPERATOR_CERT%"
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
- name: server-cm-volume
  mountPath: /weblogic-operator/server/cm
  readOnly: true
- name: server-secret-volume
  mountPath: /weblogic-operator/server/secret
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
- name: server-cm-volume
  configMap:
    defaultMode: 365
    name: {{ .domainUID }}-%TEMPLATE_NAME%-server-cm
- name: server-secret-volume
  secret:
    defaultMode: 420
    secretName: {{ .domainUID }}-%TEMPLATE_NAME%-server-secret
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
*/}}
{{- define "domain.serverPodTemplate" -}}
{{- $podName := "%SERVER_NAME%" -}}
{{- $extraContainerProperties := include "domain.serverPodTemplateExtraContainerProperties" . | fromYaml -}}
{{- $extraMetadataProperties := include "domain.serverPodTemplateExtraMetadataProperties" . | fromYaml -}}
{{- $extraLabels := include "domain.serverPodTemplateExtraLabels" . | fromYaml -}}
{{- $extraVolumeMounts := include "domain.serverPodTemplateExtraVolumeMounts" . | fromYaml -}}
{{- $extraVolumes := include "domain.serverPodTemplateExtraVolumes" . | fromYaml -}}
{{- $args := merge (dict) (omit . "extraContainerProperties" "extraVolumeMounts" "extraVolumes" ) -}}
{{- $ignore := set $args "podName" $podName -}}
{{- $ignore := set $args "extraContainerProperties" $extraContainerProperties -}}
{{- $ignore := set $args "extraMetadataProperties" $extraMetadataProperties -}}
{{- $ignore := set $args "extraLabels" $extraLabels -}}
{{- $ignore := set $args "extraVolumeMounts" $extraVolumeMounts.extraVolumeMounts -}}
{{- $ignore := set $args "extraVolumes" $extraVolumes.extraVolumes -}}
{{- include "domain.weblogicPod" $args }}
{{- end }}

{{/*
Creates a weblogic managed server pod template.
*/}}
{{- define "domain.managedServerPodTemplate" -}}
{{- include "domain.serverPodTemplate" . }}
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
*/}}
{{- define "domain.adminServerPodTemplate" -}}
{{- $extraPodSpecProperties := include "domain.adminServerPodTemplateExtraPodSpecProperties" . | fromYaml -}}
{{- $args := merge (dict) . -}}
{{- $ignore := set $args "serverType" "admin" -}}
{{- $ignore := set $args "extraPodSpecProperties" $extraPodSpecProperties -}}
{{- include "domain.serverPodTemplate" $args }}
{{- end }}
