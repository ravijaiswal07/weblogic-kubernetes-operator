{{- define "domain.domainExtraResources" }}
{{- range $idx, $element := .extraResources }}
---
{{ toYaml $element }}
{{- end }}
{{- end }}
