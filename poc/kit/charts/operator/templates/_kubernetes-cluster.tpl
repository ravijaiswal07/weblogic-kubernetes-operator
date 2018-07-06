{{- define "operator.kubernetesCluster" -}}
{{- include "operator.elasticSearchDeployment" . }}
{{- include "operator.elasticSearchService" . }}
{{- include "operator.kibanaDeployment" . }}
{{- include "operator.kibanaService" . }}
{{- include "operator.operatorClusterRole" . }}
{{- include "operator.operatorClusterRoleNamespace" . }}
{{- include "operator.operatorClusterRoleNonResource" . }}
{{- end }}
