{{- define "domain.domainExtraResources" }}
{{- range $idx, $element := .extraDomainResources }}
---
{{   toYaml $element }}
{{- end }}
{{- end }}
