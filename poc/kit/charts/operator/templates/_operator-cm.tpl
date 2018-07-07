{{- define "operator.operatorConfigMap" }}
---
apiVersion: v1
data:
  internalOperatorCert: {{ .internalOperatorCert }}
  {{- if .externalRestEnabled }}
  externalOperatorCert: {{ .externalOperatorCert }}
  {{- end }}
  serviceaccount: {{ .operatorServiceAccount }}
{{- $domainsNamespaces := merge (dict) .domainsNamespaces -}}
{{- $len := len $domainsNamespaces -}}
{{- if eq $len 0 -}}
{{-   $ignore := set $domainsNamespaces "default" (dict) -}}
{{- end }}
  targetNamespaces: {{ join "," (keys $domainsNamespaces) }}
kind: ConfigMap
metadata:
  labels:
    weblogic.operatorName: {{ .operatorNamespace }}
    weblogic.resourceVersion: operator-v1
  name: weblogic-operator-cm
  namespace: {{ .operatorNamespace }}
{{- end }}
