#!/bin/bash
export OUTPUT_DIR="/u01"
export SERVER_CM_YAML="${OUTPUT_DIR}/server-cm.yaml"
if [ -f ${SERVER_CM_YAML} ]; then
  cat ${SERVER_CM_YAML}
  exit 0
else
  echo "Error : ${SERVER_CM_YAML} not found."
  exit 1
fi
