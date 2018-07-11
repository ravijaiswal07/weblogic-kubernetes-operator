{{/*
Creates the kubernetes resources needed a managed server
*/}}
{{- define "domain.managedServerResources" -}}
{{- $scope := dict "templateName" .templateName -}}
{{- $s := .template.managedServerTemplate -}}
{{- if $s -}}
{{-   $ignore := merge $scope $s -}}
{{- end -}}
{{- $ignore := merge $scope .template -}}
{{- $s := .scope.managedServerTemplate -}}
{{- if $s -}}
{{-   $ignore := merge $scope $s -}}
{{- end -}}
{{- $ignore := merge $scope .scope -}}
managed-server-template-{{ .templateName }}.yaml: |-
{{- include "domain.managedServerPodTemplate" $scope }}
{{- include "domain.managedServerListenAddressServiceTemplate" $scope }}
{{- end }}
