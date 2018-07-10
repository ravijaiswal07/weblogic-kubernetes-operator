{{- define "domain.sitCfgGeneratorTemplateConfigMaps" }}
{{- $scope := . -}}
{{- $templates := dict "default" dict -}}
{{- $customTemplates := $scope.customSitCfgGenerators -}}
{{- if $customTemplates -}}
{{-   $ignore := merge $templates $customTemplates -}}
{{- end -}}
{{- range $key, $element := $templates -}}
{{-   $args1 := dict "current" $element "values" $scope -}}
{{-   $args2 := include "domain.getPodTemplateScope" $args1 | fromYaml -}}
{{-   $ignore := set $args2 "sitCfgName" $key -}}
{{-   include "domain.sitCfgGeneratorPod" $args2 }}
{{- end }}
{{- end }}
