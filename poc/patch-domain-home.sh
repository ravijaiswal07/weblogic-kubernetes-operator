#!/bin/bash

. pocenv.sh
set -x

cd ${DOMAIN_NAME}

# patch the domain directory
backupExtension='-zzz'
oldVal=`pwd`
newVal="${POD_DOMAIN_HOME_DIR}"
find . -type f | xargs grep -l ${oldVal} | xargs -t -n 1 sed -i ${backupExtension} "s:${oldVal}:${newVal}:g"
find . -name "*${backupExtension}" | xargs rm

# patch the java home
oldVal="${JAVA_HOME}"
newVal='/usr/java/jdk1.8.0_151'
find . -type f | xargs grep -l ${oldVal} | xargs -t -n 1 sed -i ${backupExtension} "s:${oldVal}:${newVal}:g"
find . -name "*${backupExtension}" | xargs rm

# patch the kit directory
oldVal="${MW_HOME}"
newVal="/u01/oracle"
find . -type f | xargs grep -l ${oldVal} | xargs -t -n 1 sed -i ${backupExtension} "s:${oldVal}:${newVal}:g"
find . -name "*${backupExtension}" | xargs rm

