#!/bin/sh

# WARNING: This file is created by the Configuration Wizard.
# Any changes to this script may be lost when adding extensions to this configuration.

# --- Start Functions ---

usage()
{
	echo "Need to set APPCDS_CLASS_LIST and APPCDS_ARCHIVE environment variables or specify"
	echo "them in command line:"
	echo "Usage: $1 APPCDS_CLASS_LIST {APPCDS_ARCHIVE}"
	echo "for example:"
	echo "$1 ${DOMAIN_HOME}/WebLogic.classlist ${DOMAIN_HOME}/WebLogic.jsa "
}

# --- End Functions ---

# *************************************************************************
# This script is used to generate Java application-level Class Data Sharing (AppCDS) 
# archives, which are part of an optimized class loading strategy available
# with Oracle's JDK 8u40 and above.  Starting WebLogic using archives created with
# this script will result in improved server start-up time.  If multiple WebLogic
# managed servers are started on the same physical hardware, they may share the
# same AppCDS archive resulting in lowered memory utilization since the archives
# are accessed using shared memory.
# 
# AppCDS archives are created using a class path and a class list.
# 
# The class path will be taken from the value of the CLASSPATH environment variable
# 
# The class list can be generated in two ways:
# 1. Run WebLogic in class list trial mode
#    The classes loaded by WebLogic will be dumped when the server instance exits.
#    Use startWebLogic or startManagedWebLogic with the generateClassList option.
# 2. Generate default class list containing all classes on the CLASSPATH by
#    specifying "default" for the class list when generating the archive
# 
# Note: The class path used to generate the AppCDS archive must match the class path
# used when starting WebLogic.
# 
# *************************************************************************

umask 027


USE_JVM_SYSTEM_LOADER="true"
export USE_JVM_SYSTEM_LOADER

# Call setDomainEnv here.

DOMAIN_HOME="/Users/tmoreau/op10/weblogic-kubernetes-operator/poc/demo2/generated/domain-home"

. ${DOMAIN_HOME}/bin/setDomainEnv.sh $*

if [ "$1" = "" ] ; then
	if [ "${APPCDS_CLASS_LIST}" = "" ] ; then
		usage $0
		exit
	fi
else
	APPCDS_CLASS_LIST="$1"
	export APPCDS_CLASS_LIST
	shift
fi

if [ "$1" = "" ] ; then
	if [ "${APPCDS_ARCHIVE}" = "" ] ; then
		usage $0
		exit
	fi
else
	APPCDS_ARCHIVE="$1"
	export APPCDS_ARCHIVE
	shift
fi

${JAVA_HOME}/bin/java -XX:+UnlockCommercialFeatures -XX:+UnlockDiagnosticVMOptions -Xshare:dump -XX:+UseAppCDS -XX:+IgnoreEmptyClassPaths -XX:+TraceClassPaths -XX:+IgnoreUnverifiableClassesDuringDump -XX:SharedArchiveFile=${APPCDS_ARCHIVE} -XX:SharedClassListFile=${APPCDS_CLASS_LIST}

