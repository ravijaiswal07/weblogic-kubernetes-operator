{{- define "operator.domainsNamespace" }}
{{- if (and (.createDomainsNamespace) (not (eq .domainsNamespace "default"))) }}
---
apiVersion: v1
kind: Namespace
metadata:
  labels:
    weblogic.operatorName: {{ .operatorNamespace }}
    weblogic.resourceVersion: operator-v1
  name: {{ .domainsNamespace }}
{{- end }}
{{- end }}
