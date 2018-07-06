{{- define "operator.domainsNamespaces" }}
{{- $scope := . -}}
{{- range $key, $element := .domainsNamespaces -}}
{{-   $args := merge (dict) $element $scope -}}
{{-   $ignore := set $args "domainsNamespace" $key -}}
{{-   include "operator.domainsNamespace" $args -}}
{{-   include "operator.domainConfigMap" $args -}}
{{-   include "operator.operatorRoleBinding" $args -}}
{{- end }}
{{- end }}
