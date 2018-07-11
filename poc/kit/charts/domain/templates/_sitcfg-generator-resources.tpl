{{/*
Creates the kubernetes resources needed by the situational config generator
*/}}
{{- define "domain.sitCfgGeneratorResources" -}}
{{- $scope := dict "templateName" .templateName -}}
{{- $s := .template.sitCfgGeneratorTemplate -}}
{{- if $s -}}
{{-   $ignore := merge $scope $s -}}
{{- end -}}
{{- $ignore := merge $scope .template -}}
{{- $s := .scope.sitcfgGeneratorTemplate -}}
{{- if $s -}}
{{-   $ignore := merge $scope $s -}}
{{- end -}}
{{- $ignore := merge $scope .scope -}}
sitcfg-generator-template-{{ .templateName }}.yaml: |-
{{- include "domain.sitCfgGeneratorPodTemplate" $scope }}
{{- end }}
