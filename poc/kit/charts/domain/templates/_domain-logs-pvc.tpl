{{- define "domain.domainLogsPVC" }}
{{- if .domainLogsPersistentVolumeDir }}
---
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  labels:
    weblogic.domainUID: {{ .domainUID }}
    weblogic.resourceVersion: domain-v1
  name: {{ .domainUID }}-weblogic-domain-logs-pvc
  namespace: {{ .domainsNamespace }}
spec:
  accessModes:
  - ReadWriteMany
  resources:
    requests:
      storage: 5Gi
  storageClassName: {{ .domainUID }}-weblogic-domain-logs-storage-class
  volumeName: {{ .domainUID }}-weblogic-domain-logs-pv
{{- end }}
{{- end }}
