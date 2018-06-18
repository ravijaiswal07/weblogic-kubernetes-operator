#!/bin/bash

#set -x

fileToCustomize=${1}
variableToCustomize=${2}
customValue=${!variableToCustomize}

# only replace the value if one has been specified
if [ -z $customValue ]; then
  exit
fi

backupExtension=".bak"

sed -i${backupExtension} -e "s|%${variableToCustomize}%|${customValue}|g" ${fileToCustomize}
rm ${fileToCustomize}${backupExtension}
