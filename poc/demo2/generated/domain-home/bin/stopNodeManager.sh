#!/bin/sh

# WARNING: This file is created by the Configuration Wizard.
# Any changes to this script may be lost when adding extensions to this configuration.

# *************************************************************************
#  This script is used to stop the NodeManager for this domain.
#  This script should be used only when node manager is configured per domain.
#  This script sets the following variables before stopping 
#  the node manager:
# 
#  WL_HOME    - The root directory of your WebLogic installation
#  NODEMGR_HOME  - The product name. Here it will product name and domain name
#  *************************************************************************

WL_HOME="/Users/tmoreau/depot/src122130_build/Oracle_Home/wlserver"

NODEMGR_HOME="/Users/tmoreau/op10/weblogic-kubernetes-operator/poc/demo2/generated/domain-home/nodemanager"
export NODEMGR_HOME

DOMAIN_HOME="/Users/tmoreau/op10/weblogic-kubernetes-operator/poc/demo2/generated/domain-home"

ROOT_DIRECTORY="/Users/tmoreau/op10/weblogic-kubernetes-operator/poc/demo2/generated/domain-home"
export ROOT_DIRECTORY

#  stop node manager

${WL_HOME}/server/bin/stopNodeManager.sh

