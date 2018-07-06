{{- define "operator.operator" -}}
{{- include "operator.clusterRoleBinding" . }}
{{- include "operator.clusterRoleBindingAuthDelegator" . }}
{{- include "operator.clusterRoleBindingDiscovery" . }}
{{- include "operator.clusterRoleBindingNonResource" . }}
{{- include "operator.operatorNamespace" . }}
{{- include "operator.operatorServiceAccount" . }}
{{- include "operator.operatorConfigMap" . }}
{{- include "operator.operatorSecrets" . }}
{{- include "operator.operatorDeployment" . }}
{{- include "operator.operatorInternalService" . }}
{{- include "operator.operatorExternalService" . }}
{{- include "operator.domainsNamespaces" . }}
{{- end }}
