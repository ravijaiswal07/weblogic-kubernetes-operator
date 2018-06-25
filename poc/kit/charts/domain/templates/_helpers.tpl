{{/* vim: set filetype=mustache: */}}
{{/*
Expand the name of the chart.
*/}}
{{- define "domain.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{/*
Create a default fully qualified app name.
We truncate at 63 chars because some Kubernetes name fields are limited to this (by the DNS naming spec).
If release name contains chart name it will be used as a full name.
*/}}
{{- define "domain.fullname" -}}
{{- if .Values.fullnameOverride -}}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" -}}
{{- else -}}
{{- $name := default .Chart.Name .Values.nameOverride -}}
{{- if contains $name .Release.Name -}}
{{- .Release.Name | trunc 63 | trimSuffix "-" -}}
{{- else -}}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" -}}
{{- end -}}
{{- end -}}
{{- end -}}

{{/*
Create chart name and version as used by the chart label.
*/}}
{{- define "domain.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{/*
Find an item in a map.
A dictionary must be passed in that contains a "map" struct entry and a "key" string entry.
If the map contains an entry with the same key, then this function
puts that entry in the dictionary under the "return" key.
*/}}
{{- define "domain.findMapItem" -}}
{{-   $dict := . -}}
{{-   $key := .key -}}
{{-   range $k, $e := .map -}}
{{-     if eq $k $key -}}
{{-       $ignore := set $dict "return" $e -}}
{{-     end -}}
{{-   end -}}
{{- end -}}

{{/*
Get the pod template values to use given a pod template name.
A dictionary must be passed in that contains:
 - a "name" string entry containing the name of the pod template
 - a "values" struct entry that contains the domain-level values 
This function adds a "return" dictionary entry to the dictionary.
It contains the pod template values to use.
*/}}
{{- define "domain.getPodTemplateValues" -}}
{{-   $values := .values -}}
{{-   $call := dict  "key" .name "map" $values.podTemplates -}}
{{-   include "domain.findMapItem" $call -}}
{{-   $podTemplate := $call.return -}}
{{-   $v1 := $podTemplate.weblogicImage           | default $values.weblogicImage -}}
{{-   $v2 := $podTemplate.weblogicImagePullPolicy | default $values.weblogicImagePullPolicy -}}
{{-   $v3 := $podTemplate.extraEnv                | default $values.extraEnv -}}
{{-   $v4 := $podTemplate.extraVolumes            | default $values.extraVolumes -}}
{{-   $v5 := $podTemplate.extraVolumeMounts       | default $values.extraVolumeMounts -}}
{{-   $return := dict "weblogicImage" $v1 "weblogicImagePullPolicy" $v2 "extraEnv" $v3 "extraVolumes" $v4 "extraVolumeMounts" $v5 -}}
{{-   $dict := . -}}
{{-   $ignore := set $dict "return" $return -}}
{{- end -}}
