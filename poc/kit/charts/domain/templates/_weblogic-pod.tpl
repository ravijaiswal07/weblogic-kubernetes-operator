{{/* vim: set filetype=mustache: */}}

{{/*
Get the values that should be used to create a config map containing
a pod template.
Uses the following scope variables:
  current (required) - contains any specific overrides
  current.podTemplate (optional) - a reference to a custom pod template containing more overrides
  values (required) - the domain wide default values
  values.customPodTemplates (optional) - the domain wide custom pod templates
You can think of it as creating a map containing the values from 'values',
then overlaying the values from 'current.podTemplate' (if it was specified and exists)
then finally overlaying the values from 'current'.
It returns this map as a yaml string.
*/}}
{{- define "domain.getPodTemplateScope" -}}
{{-   $scope := merge dict .current -}}
{{-   $podTemplates := .values.customPodTemplates -}}
{{-   $podTemplateName := .current.podTemplate -}}
{{-   if $podTemplateName -}}
{{-     if $podTemplates -}}
{{-       $podTemplate := index $podTemplates $podTemplateName -}}
{{-       if $podTemplate -}}
{{-         $ignore := merge $scope $podTemplate -}}
{{-       end -}}
{{-     end -}}
{{-   end -}}
{{-   merge $scope .values | toYaml -}}
{{- end -}}

{{/*
The minimum bar for creating a pod that has access to the domain home
Uses the following scope variables:
  operatorNamespace (required)
  domainUID (required)
  domainsNamespace (required)
  weblogicDomainCredentialsSecretName (required)
  configMapName (required)
  podName (required)
  yamlName (required)
  podType (required)
  podName (optional)
  weblogicImage (required)
  weblogicImagePullPolicy (required)
  weblogicimagePullSecret (optional)
  extraPodSpecProperties (optional)
  extraContainerProperties (optional)
  extraLabels (optional)
  extraVolumeMounts (optional)
  extraVolumes (optional)
  extraEnv (optional)
*/}}
{{- define "domain.weblogicPod" -}}
---
kind: ConfigMap
metadata:
  labels:
    weblogic.createdByOperator: "true"
    weblogic.operatorName: {{ .operatorNamespace }}
    weblogic.domainUID: {{ .domainUID }}
    weblogic.resourceVersion: domain-v1
  name: {{ .domainUID }}-{{ .configMapName }}
  namespace: {{ .domainsNamespace }}
data:
  {{ .yamlName }}.yaml: |-
    apiVersion: v1
    kind: Pod
    metadata:
      labels:
        weblogic.createdByOperator: "true"
        weblogic.domainUID: {{ .domainUID }}
        weblogic.resourceVersion: domain-v1
{{- if .extraLabels }}
{{ toYaml .extraLabels | trim | indent 8 }}
{{- end }}
      name: {{ .domainUID }}-{{ .podName }}
      namespace: {{ .domainsNamespace }}
{{- if .extraMetadataProperties }}
{{ toYaml .extraMetadataProperties | trim | indent 6 }}
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
{{ toYaml .extraVolumeMounts | trim | indent 8 }}
{{- end }}
{{- if .extraContainerProperties }}
{{ toYaml .extraContainerProperties | trim | indent 8 }}
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
{{ toYaml .extraVolumes | trim | indent 6 }}
{{- end }}
{{- if .extraPodSpecProperties }}
{{ toYaml .extraPodSpecProperties | trim | indent 6 }}
{{- end }}
{{- end }}
