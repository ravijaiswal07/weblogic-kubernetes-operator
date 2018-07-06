{{- define "operator.operatorNamespace" }}
{{- if (and (.createOperatorNamespace) (not (eq .operatorNamespace "default"))) }}
---
apiVersion: v1
kind: Namespace
metadata:
  labels:
    weblogic.resourceVersion: operator-v1
  name: {{ .operatorNamespace }}
{{- end }}
{{- end }}
