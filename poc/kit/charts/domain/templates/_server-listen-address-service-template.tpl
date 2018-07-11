{{- define "domain.serverListenAddressServiceTemplate" }}
  ---
  apiVersion: v1
  kind: Service
  metadata:
    annotations:
      service.alpha.kubernetes.io/tolerate-unready-endpoints: "true"
    labels:
      weblogic.createdByOperator: "true"
      weblogic.domainUID: {{ .domainUID }}
      weblogic.resourceVersion: domain-v1
      weblogic.serverName: %SERVER_NAME%
    name: {{ .domainUID }}-%SERVER_NAME%
    namespace: {{ .domainsNamespace }}
  spec:
{{- if .isAdminServer }}
    externalTrafficPolicy: Cluster
{{- end }}
    ports:
    - port: %SERVER_PORT%
      protocol: TCP
      targetPort: %SERVER_PORT%
{{- if .isAdminServer }}
      nodePort: 30901
{{- end }}
    publishNotReadyAddresses: true
    selector:
      weblogic.createdByOperator: "true"
      weblogic.domainUID: {{ .domainUID }}
      weblogic.serverName: %SERVER_NAME%
{{- if .isAdminServer }}
    type: NodePort
{{- else }}
    type: ClusterIP
{{- end }}
{{- end }}

{{/*
Creates a weblogic managed server listen address service template.
*/}}
{{- define "domain.managedServerListenAddressServiceTemplate" -}}
{{- $args := merge (dict) . -}}
{{- $ignore := set $args "isAdminServer" false -}}
{{- include "domain.serverListenAddressServiceTemplate" $args }}
{{- end }}

{{/*
Creates a weblogic admin server listen address service template.
*/}}
{{- define "domain.adminServerListenAddressServiceTemplate" -}}
{{- $args := merge (dict) . -}}
{{- $ignore := set $args "isAdminServer" true -}}
{{- include "domain.serverListenAddressServiceTemplate" $args }}
{{- end }}
