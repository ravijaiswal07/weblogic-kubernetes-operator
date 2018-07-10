{{- define "domain.adminServerT3ServiceTemplateConfigMaps" }}
{{- $scope := . -}}
{{- range $key, $element := $scope.adminServerT3ServiceTemplates -}}
---
kind: ConfigMap
metadata:
  labels:
    weblogic.createdByOperator: "true"
    weblogic.operatorName: {{ $scope.operatorNamespace }}
    weblogic.resourceVersion: domain-v1
  name: {{ $scope.domainUID }}-{{ $key }}-admin-server-t3-service-template-cm
  namespace: {{ $scope.domainsNamespace }}
data:
  server-service.yaml: |-
    apiVersion: v1
    kind: Service
    metadata:
      labels:
        weblogic.channelName: T3Channel
        weblogic.createdByOperator: "true"
        weblogic.domainUID: {{ $scope.domainUID }}
        weblogic.resourceVersion: domain-v1
        weblogic.serverName: %SERVER_NAME%
      name: {{ $scope.domainUID }}-%SERVER_NAME%-extchannel-t3channel
      namespace: {{ $scope.domainsNamespace }}
    spec:
      externalTrafficPolicy: Cluster
      ports:
      - nodePort: 30212
        port: 30212
        protocol: TCP
        targetPort: 30212
      selector:
        weblogic.createdByOperator: "true"
        weblogic.domainUID: {{ $scope.domainUID }}
        weblogic.serverName: %SERVER_NAME%
      type: NodePort
{{- end }}
{{- end }}
