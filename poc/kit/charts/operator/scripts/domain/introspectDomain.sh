#!/bin/bash
echo "Introspect the domain"
CONFIG_MAP="/u01/introspection-results.yaml"
rm -f ${CONFIG_MAP}
wlst.sh -skipWLSModuleScanning /weblogic-operator/scripts/introspect-domain.py $CONFIG_MAP

echo "Wait indefinitely so that the Kubernetes pod does not exit and try to restart"
while true; do sleep 60; done
exit 0
