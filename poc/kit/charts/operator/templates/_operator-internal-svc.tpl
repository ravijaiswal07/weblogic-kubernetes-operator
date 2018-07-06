{{- define "operator.operatorInternalService" }}
---
apiVersion: v1
kind: Service
metadata:
  name: internal-weblogic-operator-svc
  namespace: {{ .operatorNamespace }}
  labels:
    weblogic.resourceVersion: operator-v1
    weblogic.operatorName: {{ .operatorNamespace }}
spec:
  type: ClusterIP
  selector:
    app: weblogic-operator
  ports:
    - port: 8082
      name: rest
{{- end }}
