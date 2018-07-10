{{- define "domain.domainLogsPV" }}
{{- if .domainLogsPersistentVolumeDir }}
---
apiVersion: v1
kind: PersistentVolume
metadata:
  labels:
    weblogic.domainUID: {{ .domainUID }}
    weblogic.resourceVersion: domain-v1
  name: {{ .domainUID }}-weblogic-domain-logs-pv
spec:
  accessModes:
  - ReadWriteMany
  capacity:
    storage: 5Gi
  claimRef:
    apiVersion: v1
    kind: PersistentVolumeClaim
    name: {{ .domainUID }}-weblogic-domain-logs-pvc
    namespace: {{ .domainsNamespace }}
  hostPath:
    path: {{ .domainLogsPersistentVolumeDir }}
    type: ""
  persistentVolumeReclaimPolicy: Retain
  storageClassName: {{ .domainUID }}-weblogic-domain-logs-storage-class
{{- end }}
{{- end }}
