{{- define "operator.domainsNamespaces" }}
{{- $scope := . -}}
{{- $domainsNamespaces := merge (dict) .domainsNamespaces -}}
{{- $len := len $domainsNamespaces -}}
{{- if eq $len 0 -}}
{{-   $ignore := set $domainsNamespaces "default" (dict) -}}
{{- end -}}
{{- range $key, $element := $domainsNamespaces -}}
{{-   $args := merge (dict) $element $scope -}}
{{-   $ignore := set $args "domainsNamespace" $key -}}
{{-   include "operator.domainsNamespace" $args -}}
{{-   include "operator.domainConfigMap" $args -}}
{{-   include "operator.operatorRoleBinding" $args -}}
{{- end }}
{{- end }}
