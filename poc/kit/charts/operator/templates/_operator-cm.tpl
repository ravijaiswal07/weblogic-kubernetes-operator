{{- define "operator.operatorConfigMap" }}
---
apiVersion: v1
data:
  internalOperatorCert: {{ .internalOperatorCert }}
  {{- if .externalRestEnabled }}
  externalOperatorCert: {{ .externalOperatorCert }}
  {{- end }}
  serviceaccount: {{ .operatorServiceAccount }}
  targetNamespaces: {{ join "," (keys .domainsNamespaces) }}
kind: ConfigMap
metadata:
  labels:
    weblogic.operatorName: {{ .operatorNamespace }}
    weblogic.resourceVersion: operator-v1
  name: weblogic-operator-cm
  namespace: {{ .operatorNamespace }}
{{- end }}
