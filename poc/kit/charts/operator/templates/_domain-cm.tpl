{{- define "operator.domainConfigMap" }}
---
apiVersion: v1
kind: ConfigMap
metadata:
  labels:
    weblogic.createdByOperator: "true"
    weblogic.operatorName: {{ .operatorNamespace }}
    weblogic.resourceVersion: domain-v1
  name: weblogic-domain-cm
  namespace: {{ .domainsNamespace }}
data:
{{ (.Files.Glob "scripts/domain/*").AsConfig | indent 2 }}
{{- end }}
