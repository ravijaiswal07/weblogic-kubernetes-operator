#!/bin/bash

set -x

export THIS_DIR=`dirname ${BASH_SOURCE[0]}`

export TEMPLATE=$1
export OPERATOR_NAMESPACE=$2
export DOMAINS_NAMESPACE=$3
export DOMAIN_UID=$4
export DOMAIN_NAME=$5
export ADMIN_SERVER_NAME=$6
export ADMIN_SERVER_PORT=$7
export DESIRED_STATE=$8

export STARTUP_MODE=""
if [ "ADMIN" == "${DESIRED_STATE}" ]; then
  export STARTUP_MODE=" -Dweblogic.management.startupMode=ADMIN"
fi

# simulate the operator starting the admin server:

# create the kubernetes resources for the admin server
# note: the code that generates the yaml needs to process it as valid yaml.
# and sometimes the values that need to be substituted, e.g. port #s, need to
# be placeholders during that time, but need to be yaml ints later.
# we can't just use %PORT% in the template because it would be invalid yaml.
# So, we have to quote it.  But we need to let this code know to replace it,
# along with its quoting, with an int.  So, use a quoted %SERVER_PORT_AS_INT
# for these cases.
export RESOURCES_YAML=${DOMAIN_UID}-admin-server.yaml
export POD=${DOMAIN_UID}-${ADMIN_SERVER_NAME}
export INTERNAL_OPERATOR_CERT=`kubectl get cm -n ${OPERATOR_NAMESPACE} weblogic-operator-cm -o jsonpath='{.data.internalOperatorCert}'`
kubectl get cm -n ${DOMAINS_NAMESPACE} ${DOMAIN_UID}-templates-cm -o jsonpath="{.data.admin-server-template-${TEMPLATE}\.yaml}" > ${RESOURCES_YAML}
sed -i.bak \
  -e "s|%ADMIN_SERVER_NAME%|${ADMIN_SERVER_NAME}|" \
  -e "s|%ADMIN_SERVER_PORT%|${ADMIN_SERVER_PORT}|" \
  -e "s|%SERVER_NAME%|${ADMIN_SERVER_NAME}|" \
  -e "s|%SERVER_PORT%|${ADMIN_SERVER_PORT}|" \
  -e "s|'%SERVER_PORT_AS_INT%'|${ADMIN_SERVER_PORT}|" \
  -e "s|\"%SERVER_PORT_AS_INT%\"|${ADMIN_SERVER_PORT}|" \
  -e "s|%DOMAIN_NAME%|${DOMAIN_NAME}|" \
  -e "s|%TEMPLATE_NAME%|${TEMPLATE}|" \
  -e "s|%INTERNAL_OPERATOR_CERT%|${INTERNAL_OPERATOR_CERT}|" \
  -e "s|%STARTUP_MODE%|${STARTUP_MODE}|" \
${RESOURCES_YAML}
rm ${RESOURCES_YAML}.bak
kubectl apply -f ${RESOURCES_YAML}
${THIS_DIR}/wait-for-pod-to-start.sh ${DOMAINS_NAMESPACE} ${POD}
