{{- define "operator.operatorSecrets" }}
---
apiVersion: v1
data:
  internalOperatorKey: {{ .interalOperatorKey }}
  {{- if .externalRestEnabled }}
  externalOperatorKey: {{ .externalOperatorKey }}
  {{- end }}
kind: Secret
metadata:
  labels:
    weblogic.operatorName: {{ .operatorNamespace }}
    weblogic.resourceVersion: operator-v1
  name: weblogic-operator-secrets
  namespace:  {{ .operatorNamespace }}
type: Opaque
{{- end }}
