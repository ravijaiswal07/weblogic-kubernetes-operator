{{- define "operator.operatorServiceAccount" }}
{{- if (not (eq .operatorServiceAccount "default")) }}
---
apiVersion: v1
kind: ServiceAccount
metadata:
  labels:
    weblogic.operatorName: {{ .operatorNamespace }}
    weblogic.resourceVersion: operator-v1
  name: {{ .operatorServiceAccount }}
  namespace: {{ .operatorNamespace }}
{{- end }}
{{- end }}
