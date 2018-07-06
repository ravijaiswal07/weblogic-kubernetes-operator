{{- define "operator.operatorClusterRole" }}
---
kind: ClusterRole
apiVersion: rbac.authorization.k8s.io/v1beta1
metadata:
  name: weblogic-operator-cluster-role
  labels:
    weblogic.resourceVersion: operator-v1
rules:
- apiGroups: [""]
  resources: ["namespaces"]
  verbs: ["get", "list", "watch"]
- apiGroups: [""]
  resources: ["persistentvolumes"]
  verbs: ["get", "list", "watch", "create", "update", "patch", "delete", "deletecollection"]
- apiGroups: ["apiextensions.k8s.io"]
  resources: ["customresourcedefinitions"]
  verbs: ["get", "list", "watch", "create", "update", "patch", "delete", "deletecollection"]
- apiGroups: ["weblogic.oracle"]
  resources: ["domains"]
  verbs: ["get", "list", "watch", "update", "patch"]
- apiGroups: ["weblogic.oracle"]
  resources: ["domains/status"]
  verbs: ["update"]
- apiGroups: ["extensions"]
  resources: ["ingresses"]
  verbs: ["get", "list", "watch", "create", "update", "patch", "delete", "deletecollection"]
- apiGroups: ["authentication.k8s.io"]
  resources: ["tokenreviews"]
  verbs: ["create"]
- apiGroups: ["authorization.k8s.io"]
  resources: ["selfsubjectaccessreviews", "localsubjectaccessreviews", "subjectaccessreviews", "selfsubjectrulesreviews"]
  verbs: ["create"]
{{- end }}
