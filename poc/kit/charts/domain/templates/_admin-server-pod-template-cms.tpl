{{- define "domain.adminServerPodTemplateConfigMaps" }}
{{- $scope := . -}}
{{- $templates := dict "default" dict -}}
{{- $customTemplates := $scope.customAdminServerPodTemplates -}}
{{- if $customTemplates -}}
{{-   $ignore := merge $templates $customTemplates -}}
{{- end -}}
{{- range $key, $element := $templates -}}
{{-   $args1 := dict "current" $element "values" $scope -}}
{{-   $args2 := include "domain.getPodTemplateScope" $args1 | fromYaml -}}
{{-   $ignore := set $args2 "templateName" $key -}}
{{-   include "domain.adminServerPodTemplate" $args2 }}
{{- end }}
{{- end }}
