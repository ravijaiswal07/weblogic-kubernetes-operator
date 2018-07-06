{{- define "operator.operatorExternalService" }}
{{- if (or .externalRestEnabled .remoteDebugNodePortEnabled) }}
---
apiVersion: v1
kind: Service
metadata:
  name: external-weblogic-operator-svc
  namespace: {{ .operatorNamespace }}
  labels:
    weblogic.resourceVersion: operator-v1
    weblogic.operatorName: {{ .operatorNamespace }}
spec:
  type: NodePort
  selector:
    app: weblogic-operator
  ports:
    {{- if .externalRestEnabled }}
    - name: rest
      port: 8081
      nodePort: {{ .externalRestHttpsPort }}
    {{- end }}
    {{- if .remoteDebugNodePortEnabled }}
    - name: debug
      port: {{ .internalDebugHttpPort }}
      nodePort: {{ .externalDebugHttpPort }}
    {{- end }}
{{- end }}
{{- end }}
