{{- define "domain.templateResources" }}
{{ include "domain.domainIntrospectorResources" . }}
{{ include "domain.sitCfgGeneratorResources" . }}
{{ include "domain.adminServerResources" . }}
{{ include "domain.managedServerResources" . }}
{{- end -}}
