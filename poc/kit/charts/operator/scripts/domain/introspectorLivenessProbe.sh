#!/bin/bash
CONFIG_MAP="/u01/introspection-results.yaml"
if [ -f $CONFIG_MAP ]; then
  exit 0
else
  echo "Error : ${CONFIG_MAP} not found."
  exit 1
fi

