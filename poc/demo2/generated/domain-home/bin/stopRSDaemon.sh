#!/bin/sh

# WARNING: This file is created by the Configuration Wizard.
# Any changes to this script may be lost when adding extensions to this configuration.

# *************************************************************************
#  This script is used to stop a Replicated Store Daemon.
#  This script should be used only when a Replicated Store is configured for a domain.
#  If JAVA_HOME is not set, setDomainEnv is called to initialize JAVA_HOME and other variables (see setDomainEnv.sh).
#  *************************************************************************

WL_HOME="/Users/tmoreau/depot/src122130_build/Oracle_Home/wlserver"
export WL_HOME

DOMAIN_HOME="/Users/tmoreau/op10/weblogic-kubernetes-operator/poc/demo2/generated/domain-home"
export DOMAIN_HOME

#  stop RSDaemon, this will call setDomainEnv first if JAVA_HOME is not set

${WL_HOME}/server/bin/stopRSDaemon.sh $@

