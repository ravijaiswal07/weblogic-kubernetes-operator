#!/bin/bash
export OUTPUT_DIR="/u01"
export TOPOLOGY_YAML="${OUTPUT_DIR}/topology.yaml"
if [ -f ${TOPOLOGY_YAML} ]; then
  cat ${TOPOLOGY_YAML}
  exit 0
else
  echo "Error : ${TOPOLOGY_YAML} not found."
  exit 1
fi
