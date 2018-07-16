#!/bin/bash
export OUTPUT_DIR="/u01"
export SERVER_SECRET_YAML="${OUTPUT_DIR}/server-secret.yaml"
if [ -f ${SERVER_SECRET_YAML} ]; then
  cat ${SERVER_SECRET_YAML}
  exit 0
else
  echo "Error : ${SERVER_SECRET_YAML} not found."
  exit 1
fi
