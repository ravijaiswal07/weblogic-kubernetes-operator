{{- define "domain.templatesConfigMap" -}}
{{- $scope := . -}}
kind: ConfigMap
metadata:
  labels:
    weblogic.createdByOperator: "true"
    weblogic.operatorName: {{ $scope.operatorNamespace }}
    weblogic.domainUID: {{ $scope.domainUID }}
    weblogic.resourceVersion: domain-v1
  name: {{ $scope.domainUID }}-templates-cm
  namespace: {{ $scope.domainsNamespace }}
data:
{{- $templates := dict "default" dict -}}
{{- $customTemplates := $scope.customTemplates -}}
{{- if $customTemplates -}}
{{-   $ignore := merge $templates $customTemplates -}}
{{- end -}}
{{- range $key, $element := $templates -}}
{{-   $args := dict "templateName" $key "template" $element "scope" $scope -}}
{{-   include "domain.templateResources" $args | indent 2 }}
{{- end }}
{{- end }}
