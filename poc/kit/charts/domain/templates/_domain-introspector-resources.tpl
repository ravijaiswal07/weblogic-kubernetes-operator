{{/*
Creates the kubernetes resources needed by the domain introspector
*/}}
{{- define "domain.domainIntrospectorResources" -}}
{{- $scope := dict "templateName" .templateName -}}
{{- $s := .template.domainIntrospectorTemplate -}}
{{- if $s -}}
{{-   $ignore := merge $scope $s -}}
{{- end -}}
{{- $ignore := merge $scope .template -}}
{{- $s := .scope.domainIntrospectorTemplate -}}
{{- if $s -}}
{{-   $ignore := merge $scope $s -}}
{{- end -}}
{{- $ignore := merge $scope .scope -}}
domain-introspector-template-{{ .templateName }}.yaml: |-
{{- include "domain.domainIntrospectorPodTemplate" $scope }}
{{- end }}
