#!/bin/bash

. %SETUP_ENV_SCRIPT%

#set -x

SERVER_NAME="${1}"

cat ${DOMAIN_LOGS_PV_DIR}/${SERVER_NAME}.log
