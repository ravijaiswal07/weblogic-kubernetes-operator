{{/*
Creates the kubernetes resources needed the admin server
*/}}
{{- define "domain.adminServerResources" -}}
{{- $scope := dict "templateName" .templateName -}}
{{- $s := .template.adminServerTemplate -}}
{{- if $s -}}
{{-   $ignore := merge $scope $s -}}
{{- end -}}
{{- $ignore := merge $scope .template -}}
{{- $s := .scope.adminServerTemplate -}}
{{- if $s -}}
{{-   $ignore := merge $scope $s -}}
{{- end -}}
{{- $ignore := merge $scope .scope -}}
admin-server-template-{{ .templateName }}.yaml: |-
{{- include "domain.adminServerPodTemplate" $scope }}
{{- include "domain.adminServerListenAddressServiceTemplate" $scope }}
{{- end }}
