#!/bin/bash

. %SETUP_ENV_SCRIPT%

#set -x

SERVER_NAME="${1}"

cat ${DOMAIN_LOGS_PV_DIR}/nodemanager-${SERVER_NAME}.log
