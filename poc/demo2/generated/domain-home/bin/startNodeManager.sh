#!/bin/sh

# WARNING: This file is created by the Configuration Wizard.
# Any changes to this script may be lost when adding extensions to this configuration.

# *************************************************************************
#  This script is used to start a NodeManager.
#  This script should be used only when node manager is configured per domain.
#  This script sets the following variables before starting 
#  the node manager:
# 
#  WL_HOME    - The root directory of your WebLogic installation
#  NODEMGR_HOME  - The product name. Here it will product name and domain name
#  *************************************************************************

WL_HOME="/Users/tmoreau/depot/src122130_build/Oracle_Home/wlserver"

NODEMGR_HOME="/Users/tmoreau/op10/weblogic-kubernetes-operator/poc/demo2/generated/domain-home/nodemanager"
export NODEMGR_HOME

DOMAIN_HOME="/Users/tmoreau/op10/weblogic-kubernetes-operator/poc/demo2/generated/domain-home"

JAVA_OPTIONS="${JAVA_OPTIONS} -Dweblogic.RootDirectory=${DOMAIN_HOME} "
export JAVA_OPTIONS

#  Set JAVA_HOME for node manager

. ${DOMAIN_HOME}/bin/setNMJavaHome.sh

#  start node manager

${WL_HOME}/server/bin/startNodeManager.sh

