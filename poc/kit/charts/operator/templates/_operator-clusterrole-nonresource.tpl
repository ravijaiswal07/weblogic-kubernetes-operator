{{- define "operator.operatorClusterRoleNonResource" }}
---
kind: ClusterRole
apiVersion: rbac.authorization.k8s.io/v1beta1
metadata:
  name: weblogic-operator-cluster-role-nonresource
  labels:
    weblogic.resourceVersion: operator-v1
rules:
- nonResourceURLs: ["/version/*"]
  verbs: ["get"]
{{- end }}
