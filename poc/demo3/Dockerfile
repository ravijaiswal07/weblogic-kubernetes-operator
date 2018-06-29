# LICENSE CDDL 1.0 + GPL 2.0
#
# Copyright (c) 2017,2018 Oracle and/or its affiliates. All rights reserved.
#
# ORACLE DOCKERFILES PROJECT
# --------------------------
# This Dockerfile extends the Oracle WebLogic image by creating an domain on which
# a managed server can be launched in Managed Server Independence (MSI) mode
#
# REQUIRED FILES TO BUILD THIS IMAGE
# ----------------------------------
#
# HOW TO BUILD THIS IMAGE
# -----------------------
# Run: 
#      $ sudo docker build -t wls-12213-domain .
#

# Pull base image
# ---------------
FROM store/oracle/weblogic:12.2.1.3 

# Maintainer
# ----------
MAINTAINER Lenny Phan <lenny.phan@oracle.com>

ARG MS_PORT=8200
ARG AS_PORT=7200
ARG NM_PORT=5556
ARG DOMAIN_HOME_DIR="/u01/oracle/domain-home"

# Environment variables required for this build (do NOT change)
# -------------------------------------------------------------
ENV MW_HOME="$ORACLE_HOME" \
    POD_DOMAIN_DIR="$DOMAIN_HOME_DIR" \
    MANAGED_SERVER_PORT=$MS_PORT \
    ADMIN_SERVER_PORT=$AS_PORT \
    NODE_MANAGER_PORT=$NM_PORT \
    PATH="$ORACLE_HOME/wlserver/server/bin:$ORACLE_HOME/wlserver/../oracle_common/modules/org.apache.ant_1.9.2/bin:$JAVA_HOME/jre/bin:$JAVA_HOME/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin:$ORACLE_HOME/oracle_common/common/bin:$ORACLE_HOME/wlserver/common/bin:$ORACLE_HOME/user_projects/domains/medrec/bin:$ORACLE_HOME/wlserver/samples/server/medrec/:$ORACLE_HOME/wlserver/samples/server/:$ORACLE_HOME/wlserver/../oracle_common/modules/org.apache.maven_3.2.5/bin"

# Copy scripts
# --------------------------------
USER oracle
COPY create-domain-home-with-configured-cluster.sh $MW_HOME/
COPY create-domain-home.sh $MW_HOME/
CMD chmod +x $MW_HOME/*.sh

# Default directory creation, Admin Server boot
# ---------------------------------------------
RUN . $ORACLE_HOME/wlserver/server/bin/setWLSEnv.sh && \
    cd $MW_HOME && \
    ./create-domain-home.sh

EXPOSE $MANAGED_SERVER_PORT $ADMIN_SERVER_PORT $NODE_MANAGER_PORT
WORKDIR $POD_DOMAIN_DIR
