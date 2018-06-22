#!/bin/sh

# WARNING: This file is created by the Configuration Wizard.
# Any changes to this script may be lost when adding extensions to this configuration.

if [ "$1" != "" ] ; then
	wlsUserID="$1"
	export wlsUserID
	userID="username=wlsUserID,"
	shift
else
	if [ "${userID}" != "" ] ; then
		wlsUserID="${userID}"
		export wlsUserID
		userID="username=wlsUserID,"
	fi
fi

if [ "$1" != "" ] ; then
	wlsPassword="$1"
	export wlsPassword
	password="password=wlsPassword,"
	shift
else
	if [ "${password}" != "" ] ; then
		wlsPassword="${password}"
		export wlsPassword
		password="password=wlsPassword,"
	fi
fi

# set ADMIN_URL

if [ "$1" != "" ] ; then
	ADMIN_URL="$1"
	shift
else
	if [ "${ADMIN_URL}" = "" ] ; then
		ADMIN_URL="t3://tmoreau-mac.local:7100"
	fi
fi

# Call setDomainEnv here because we want to have shifted out the environment vars above

DOMAIN_HOME="/Users/tmoreau/op10/weblogic-kubernetes-operator/poc/demo2/generated/domain-home"

# Read the environment variable from the console.

if [ "${doExit}" = "true" ] ; then
	exitFlag="doExit"
else
	exitFlag="noExit"
fi

. ${DOMAIN_HOME}/bin/setDomainEnv.sh stopcmd $*

umask 026


echo "wlsUserID = java.lang.System.getenv('wlsUserID')" >"shutdown-${SERVER_NAME}.py" 
echo "wlsPassword = java.lang.System.getenv('wlsPassword')" >>"shutdown-${SERVER_NAME}.py" 
echo "connect(${userID} ${password} url='${ADMIN_URL}', adminServerName='${SERVER_NAME}')" >>"shutdown-${SERVER_NAME}.py" 
echo "shutdown('${SERVER_NAME}','Server', ignoreSessions='true')" >>"shutdown-${SERVER_NAME}.py" 
echo "exit()" >>"shutdown-${SERVER_NAME}.py" 

echo "Stopping Weblogic Server..."

${JAVA_HOME}/bin/java -classpath ${FMWCONFIG_CLASSPATH} ${MEM_ARGS} ${JVM_D64} ${JAVA_OPTIONS} weblogic.WLST shutdown-${SERVER_NAME}.py  2>&1 

shutDownStatus=$?


echo "Done"

echo "Stopping Derby Server..."

if [ "${DERBY_FLAG}" = "true" ] ; then
	. ${WL_HOME}/common/derby/bin/stopNetworkServer.sh  >"${DOMAIN_HOME}/derbyShutdown.log" 2>&1 
	echo "Derby server stopped."
fi

if [ "${shutDownStatus}" != "0" ] ; then
	exit 1
fi

# Exit this script only if we have been told to exit.

if [ "${doExitFlag}" = "true" ] ; then
	exit
fi

