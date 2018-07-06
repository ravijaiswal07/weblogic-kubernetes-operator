{{- define "operator.clusterRoleBindingDiscovery" }}
---
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRoleBinding
metadata:
  labels:
    weblogic.operatorName: {{ .operatorNamespace }}
    weblogic.resourceVersion: operator-v1
  name: {{ .operatorNamespace }}-operator-rolebinding-discovery
roleRef:
  apiGroup: rbac.authorization.k8s.io
  kind: ClusterRole
  name: system:discovery
subjects:
- kind: ServiceAccount
  name: {{ .operatorServiceAccount }}
  namespace: {{ .operatorNamespace }}
{{- end }}
