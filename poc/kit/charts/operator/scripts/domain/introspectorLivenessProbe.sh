#!/bin/bash
export OUTPUT_DIR="/u01"
export TOPOLOGY_YAML="${OUTPUT_DIR}/topology.yaml"
if [ -f $TOPOLOGY_YAML ]; then
  exit 0
fi
echo "${TOPOLOGY_YAML} not found."
exit 1

