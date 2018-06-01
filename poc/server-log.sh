#!/bin/bash

. pocenv.sh
set -x

SERVER_NAME="${1}"
cat ${DOMAIN_LOGS_PV_DIR}/${SERVER_NAME}.log
