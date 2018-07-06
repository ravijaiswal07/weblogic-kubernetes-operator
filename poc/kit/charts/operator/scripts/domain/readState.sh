#!/bin/bash

# Reads the current state of a server. The script checks a WebLogic Server state
# file which is updated by the node manager.

STATEFILE=${DOMAIN_HOME}/servers/${SERVER_NAME}/data/nodemanager/${SERVER_NAME}.state

if [ `jps -l | grep -c " weblogic.NodeManager"` -eq 0 ]; then
  echo "Error: WebLogic NodeManager process not found."
  exit 1
fi

if [ ! -f ${STATEFILE} ]; then
  echo "Error: WebLogic Server state file not found."
  exit 2
fi

cat ${STATEFILE} | cut -f 1 -d ':'
exit 0
