{{/*
The minimum bar for creating a pod that has access to the domain home
*/}}
{{- define "domain.weblogicPod" }}
  ---
  apiVersion: v1
  kind: Pod
  metadata:
    labels:
      weblogic.createdByOperator: "true"
      weblogic.domainUID: {{ .domainUID }}
      weblogic.resourceVersion: domain-v1
{{- if .extraLabels }}
{{ toYaml .extraLabels | trim | indent 6 }}
{{- end }}
    name: {{ .domainUID }}-{{ .podName }}
    namespace: {{ .domainsNamespace }}
{{- if .extraMetadataProperties }}
{{ toYaml .extraMetadataProperties | trim | indent 4 }}
{{- end }}
  spec:
    containers:
    - image: {{ .weblogicImage }}
      imagePullPolicy: {{ .weblogicImagePullPolicy }}
      name: weblogic-server
      volumeMounts:
      - name: weblogic-credentials-volume
        mountPath: /weblogic-operator/secrets
        readOnly: true
      - name: weblogic-domain-cm-volume
        mountPath: /weblogic-operator/scripts
        readOnly: true
{{- if .extraVolumeMounts }}
{{ toYaml .extraVolumeMounts | trim | indent 6 }}
{{- end }}
{{- if .extraContainerProperties }}
{{ toYaml .extraContainerProperties | trim | indent 6 }}
{{- end }}
    volumes:
    - name: weblogic-credentials-volume
      secret:
        defaultMode: 420
        secretName:  {{ .weblogicDomainCredentialsSecretName }}
    - name: weblogic-domain-cm-volume
      configMap:
        defaultMode: 365
        name: weblogic-domain-cm 
{{- if .extraVolumes }}
{{ toYaml .extraVolumes | trim | indent 4 }}
{{- end }}
{{- if .extraPodSpecProperties }}
{{ toYaml .extraPodSpecProperties | trim | indent 4 }}
{{- end }}
{{- end }}
