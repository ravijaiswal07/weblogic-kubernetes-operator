{{/* vim: set filetype=mustache: */}}
{{/*
Verify that an input value of a specific kind has been specified.
*/}}
{{- define "domain.verifyInputKind" -}}
{{- $requiredKind := index . 0 -}}
{{- $scope := index . 1 -}}
{{- $value := index . 1 -}}
{{- $name := index . 2 -}}
{{- if not ( hasKey $scope $name ) -}}
{{-   $errorMsg := cat "The" $requiredKind "property" $name "must be specified." -}}
{{-   $ignore := required $errorMsg "" -}}
{{- end -}}
{{- $value := index $scope $name -}}
{{- $actualKind := kindOf $value -}}
{{- if not ( eq $requiredKind $actualKind ) -}}
{{-   $errorMsg := cat "The" $actualKind "property" $name "must be a" $requiredKind "instead." -}}
{{-   $ignore := required $errorMsg "" -}}
{{- end -}}
{{- end -}}

{{/*
Verify that a string input value has been specified
*/}}
{{- define "domain.verifyStringInput" -}}
{{- include "operator.verifyInputKind" ( list "string" ( index . 0 ) ( index . 1 ) ) -}} 
{{- end -}}

{{/*
Verify that a boolean input value has been specified
*/}}
{{- define "domain.verifyBooleanInput" -}}
{{- include "operator.verifyInputKind" ( list "bool" ( index . 0 ) ( index . 1 ) ) -}} 
{{- end -}}

{{/*
Verify that an integer input value has been specified
*/}}
{{- define "domain.verifyIntegerInput" -}}
{{- include "operator.verifyInputKind" ( list "float64" ( index . 0 ) ( index . 1 ) ) -}} 
{{- end -}}

{{/*
Verify that an object input value has been specified
*/}}
{{- define "domain.verifyObjectInput" -}}
{{- include "operator.verifyInputKind" ( list "map" ( index . 0 ) ( index . 1 ) ) -}} 
{{- end -}}

{{/*
Verify that an enum string input value has been specified
*/}}
{{- define "domain.verifyEnumInput" -}}
{{- $scope := index . 0 -}}
{{- $name := index . 1 -}}
{{- $legalValues := index . 2 -}}
{{- include "operator.verifyStringInput" ( list $scope $name ) -}}
{{- $value := index $scope $name -}}
{{- if not ( has $value $legalValues ) -}}
{{    $errorMsg := cat "The property" $name "must be one of following values" $legalValues "instead of" $value -}}
{{-   $ignore := required $errorMsg "" -}}
{{- end -}}
{{- end -}}
