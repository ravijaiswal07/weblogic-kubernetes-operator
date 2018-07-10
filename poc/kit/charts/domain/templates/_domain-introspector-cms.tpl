{{- define "domain.domainIntrospectorTemplateConfigMaps" }}
{{- $scope := . -}}
{{- $templates := dict "default" dict -}}
{{- $customTemplates := $scope.customDomainIntrospectors -}}
{{- if $customTemplates -}}
{{-   $ignore := merge $templates $customTemplates -}}
{{- end -}}
{{- range $key, $element := $templates -}}
{{-   $args1 := dict "current" $element "values" $scope -}}
{{-   $args2 := include "domain.getPodTemplateScope" $args1 | fromYaml -}}
{{-   $ignore := set $args2 "introspectorName" $key -}}
{{-   include "domain.domainIntrospectorPod" $args2 }}
{{- end }}
{{- end }}
