#!/bin/sh

# WARNING: This file is created by the Configuration Wizard.
# Any changes to this script may be lost when adding extensions to this configuration.

# *************************************************************************
#  This script starts a Replicated Store Daemon.
#  It should only be used when a Replicated Store is configured in a domain.
#  This script sets the following variables before starting 
#  a Daemon:
# 
#  WL_HOME    - The root directory of your WebLogic installation
#  *************************************************************************

WL_HOME="/Users/tmoreau/depot/src122130_build/Oracle_Home/wlserver"
export WL_HOME

#  start RSDaemon

${WL_HOME}/server/bin/startRSDaemon.sh $@

