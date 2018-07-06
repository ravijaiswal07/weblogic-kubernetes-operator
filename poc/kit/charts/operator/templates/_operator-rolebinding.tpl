{{- define "operator.operatorRoleBinding" }}
---
kind: RoleBinding
apiVersion: rbac.authorization.k8s.io/v1beta1
metadata:
  name: weblogic-operator-rolebinding
  namespace: {{ .domainsNamespace }}
  labels:
    weblogic.resourceVersion: operator-v1
    weblogic.operatorName: {{ .operatorNamespace }}
subjects:
- kind: ServiceAccount
  name: {{ .operatorServiceAccount }}
  namespace: {{ .operatorNamespace }}
  apiGroup: ""
roleRef:
  kind: ClusterRole
  name: weblogic-operator-namespace-role
  apiGroup: ""
{{- end }}
