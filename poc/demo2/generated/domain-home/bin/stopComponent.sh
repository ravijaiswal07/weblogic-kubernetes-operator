#!/bin/sh

# WARNING: This file is created by the Configuration Wizard.
# Any changes to this script may be lost when adding extensions to this configuration.

# --- Start Functions ---

usage()
{
	echo "Usage: $1 {help} COMPONENT_NAME {showErrorStack}"
	echo "Where:"
	echo "  help           - Optional. Show this usage."
	echo "  COMPONENT_NAME - Required. System Component name, only one name allowed"
	echo "  showErrorStack - Optional. Show error stack if provided."
}

# --- End Functions ---

if [ "$1" = "" ] ; then
	usage $0
	exit
fi
param="$(echo $1 | tr -s '')"
if [ "${param}" = "" ] ; then
	usage $0
	exit
fi


if [ "$1" = "showErrorStack" ] ; then
	usage $0
	exit
fi

showErrorStack="false"
doUsage="false"
while [ $# -gt 0 ]
do
	case $1 in
	showErrorStack)
		showErrorStack="true"
		export showErrorStack
		;;
	help)
		doUsage="true"
		;;
	*)
		if [ "${componentName}" != "" ] ; then
			usage $0
			exit
		fi
		componentName="$1"
		export componentName
		;;
	esac
	shift
done


if [ "${doUsage}" = "true" ] ; then
	usage $0
	exit
fi

WL_HOME="/Users/tmoreau/depot/src122130_build/Oracle_Home/wlserver"

DOMAIN_HOME="/Users/tmoreau/op10/weblogic-kubernetes-operator/poc/demo2/generated/domain-home"

if [ "${TMPDIR}" != "" ] ; then
	PY_LOC="${TMPDIR}/stopComponent.py"
else
	PY_LOC="/tmp/stopComponent.py"
fi


umask 027


if [ "${showErrorStack}" = "false" ] ; then
	echo "try:" >"${PY_LOC}" 
	echo "  stopComponentInternal('${componentName}', r'${DOMAIN_HOME}')" >>"${PY_LOC}" 
	echo "  exit()" >>"${PY_LOC}" 
	echo "except Exception,e:" >>"${PY_LOC}" 
	echo "  print 'Error:', sys.exc_info()[1]" >>"${PY_LOC}" 
	echo "  exit()" >>"${PY_LOC}" 
else
	echo "stopComponentInternal('${componentName}', r'${DOMAIN_HOME}')" >"${PY_LOC}" 
	echo "exit()" >>"${PY_LOC}" 
fi

echo "Stopping System Component ${componentName} ..."

# Using WLST...

${WL_HOME}/../oracle_common/common/bin/wlst.sh -i ${PY_LOC}  2>&1 

if [ -f ${PY_LOC} ] ; then

	rm -f ${PY_LOC}

fi

echo "Done"

exit

