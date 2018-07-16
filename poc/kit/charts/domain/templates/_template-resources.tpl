{{- define "domain.templateResources" }}
{{ include "domain.domainIntrospectorResources" . }}
{{ include "domain.adminServerResources" . }}
{{ include "domain.managedServerResources" . }}
{{- end -}}
