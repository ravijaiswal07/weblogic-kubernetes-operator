#!/bin/bash
echo "Generate situational config"
CONFIG_MAP="/u01/generate-sitcfg-results.yaml"
rm -f ${CONFIG_MAP}
wlst.sh -skipWLSModuleScanning /weblogic-operator/scripts/generate-sit-cfg.py $CONFIG_MAP

echo "Wait indefinitely so that the Kubernetes pod does not exit and try to restart"
while true; do sleep 60; done
exit 0
