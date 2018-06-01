#!/bin/bash

. pocenv.sh
set -x

mkdir -p ${DOMAIN_LOGS_PV_DIR}
mkdir -p ${DOMAIN_HOME_PV_DIR}
cd ${DOMAIN_NAME}
cp -r . ${DOMAIN_HOME_PV_DIR}
