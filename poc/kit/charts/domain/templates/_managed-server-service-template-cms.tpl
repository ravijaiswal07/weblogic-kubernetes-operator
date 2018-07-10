{{- define "domain.managedServerServiceTemplateConfigMaps" }}
{{- $scope := . -}}
{{- range $key, $element := $scope.managedServerServiceTemplates -}}
---
kind: ConfigMap
metadata:
  labels:
    weblogic.createdByOperator: "true"
    weblogic.operatorName: {{ $scope.operatorNamespace }}
    weblogic.resourceVersion: domain-v1
  name: {{ $scope.domainUID }}-{{ $key }}-managed-server-service-template-cm
  namespace: {{ $scope.domainsNamespace }}
data:
  server-service.yaml: |-
    apiVersion: v1
    kind: Service
    metadata:
      annotations:
        service.alpha.kubernetes.io/tolerate-unready-endpoints: "true"
      labels:
        weblogic.createdByOperator: "true"
        weblogic.domainUID: {{ $scope.domainUID }}
        weblogic.resourceVersion: domain-v1
        weblogic.serverName: %SERVER_NAME%
      name: {{ $scope.domainUID }}-%SERVER_NAME%
      namespace: {{ $scope.domainsNamespace }}
    spec:
      ports:
      - port: %SERVER_PORT%
        protocol: TCP
        targetPort: %SERVER_PORT%
      publishNotReadyAddresses: true
      selector:
        weblogic.createdByOperator: "true"
        weblogic.domainUID: {{ $scope.domainUID }}
        weblogic.serverName: %SERVER_NAME%
      type: ClusterIP
{{- end }}
{{- end }}
