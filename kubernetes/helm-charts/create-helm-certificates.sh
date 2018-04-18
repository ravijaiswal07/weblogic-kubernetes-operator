#!/usr/bin/env bash
# Copyright 2017, 2018, Oracle Corporation and/or its affiliates.  All rights reserved.
# Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.

script_dir="$( cd "$( dirname "${BASH_SOURCE[0]}" )"/.. && pwd )"

input="$script_dir/helm-charts/weblogic-operator/values-template.yaml"
output="$script_dir/helm-charts/weblogic-operator/values.yaml"
if [ $# -eq 1 ]; then
  namespace=$1
  external_rest="none"
elif [ $# -eq 2 ] || [ $# -eq 4 ]; then
  namespace=$1
  external_rest="self-signed-cert"
  external_sans=$2
  if [ $# -eq 4 ]; then
    input=$3
    output=$4
  fi
  echo "Generating a self-signed certificate for the operator's external ssl port with the subject alternative names ${external_sans}"
  $script_dir/internal/generate-weblogic-operator-cert.sh $external_sans
  external_cert_dir="$script_dir/internal/external-weblogic-operator-cert"
  external_cert_file="${external_cert_dir}/weblogic-operator.cert.pem"
  external_key_file="${external_cert_dir}/weblogic-operator.key.pem"
  mv $script_dir/internal/weblogic-operator-cert $external_cert_dir
elif [ $# -eq 3 ] || [ $# -eq 5 ]; then
  external_rest="custom-cert"
  namespace=$1
  external_cert_file=$2
  external_key_file=$3
  if [ $# -eq 5 ]; then
    input=$4
    output=$5
  fi
  if [ ! -f $external_cert_file ]; then
    echo "$external_cert_file does not exist"
    exit 1
  fi
  if [ ! -f $external_key_file ]; then
    echo "$external_key_file does not exist"
    exit 1
  fi
  echo "Using the customer supplied external operator certificate stored in ${external_cert_file} and private key stored in ${external_key_file}"
else
  echo "Syntax:"
  echo "    ${BASH_SOURCE[0]} <namespace>"
  echo "        Creates a WebLogic operator in the customer-provided Kubernetes namespace."
  echo "        Does not externally expose the WebLogic Operator REST interface."
  echo "    ${BASH_SOURCE[0]} <namespace> <subject-alternative-names (e.g. DNS:myhost,DNS:localhost,IP:127.0.0.1)>"
  echo "        Creates a WebLogic operator in the customer-provided Kubernetes namespace."
  echo "        Externally exposes the WebLogic operator's REST interface, using a generated self-signed"
  echo "        certificate that contains the customer-provided list of subject alternative names."
  echo "    ${BASH_SOURCE[0]} <namespace> <operator-certificate-pem-file-pathname> <operator-private-key-pem-file-pathname>"
  echo "        Creates a WebLogic operator in the customer-provided Kubernetes namespace."
  echo "        Externally exposes the WebLogic operator'sREST interface, using a customer-provided"
  echo "        certificate and private key pair."
  exit
fi

# Always generate a self-signed cert for the internal operator REST port
internal_host="internal-weblogic-operator-service"
internal_sans="DNS:${internal_host},DNS:${internal_host}.${namespace},DNS:${internal_host}.${namespace}.svc,DNS:${internal_host}.${namespace}.svc.cluster.local"
echo "Generating a self-signed certificate for the operator's internal https port with the subject alternative names ${internal_sans}"
$script_dir/internal/generate-weblogic-operator-cert.sh $internal_sans

internal_cert_dir="$script_dir/internal/weblogic-operator-cert"
internal_cert_file="${internal_cert_dir}/weblogic-operator.cert.pem"
internal_key_file="${internal_cert_dir}/weblogic-operator.key.pem"

if [ $external_rest == "none" ]; then
  external_cert_data="\"\""
  external_key_data="\"\""
else
  external_cert_data=`base64 -i ${external_cert_file} | tr -d '\n'`
  external_key_data=`base64 -i ${external_key_file} | tr -d '\n'`
  rm -rf $external_cert_dir
fi

# internal_cert_data and internal
internal_cert_data=`base64 -i ${internal_cert_file} | tr -d '\n'`
internal_key_data=`base64 -i ${internal_key_file} | tr -d '\n'`
rm -rf $internal_cert_dir

# Validate the template files exist
if [ ! -f ${input} ]; then
  echo The file ${input} was not found
fi

echo ""
echo Reading from template file:
echo - ${input}
echo ""
echo Generating file:
echo - ${output}

# Create the output files
cp ${input} ${output}

# Do the edits
sed -i -e "s/%EXTERNAL_CERT_DATA%/$external_cert_data/g" ${output}
sed -i -e "s/%EXTERNAL_KEY_DATA%/$external_key_data/g" ${output}
sed -i -e "s/%INTERNAL_CERT_DATA%/$internal_cert_data/g" ${output}
sed -i -e "s/%INTERNAL_KEY_DATA%/$internal_key_data/g" ${output}

echo ""
echo Completed




