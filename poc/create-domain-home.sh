#!/bin/bash

. pocenv.sh
set -x

rm -rf ${DOMAIN_NAME}

java weblogic.WLST << EOF

domain_uid               = "${DOMAIN_UID}"
domain_name              = "${DOMAIN_NAME}"
domain_path              = domain_name
admin_username           = "${ADMIN_USERNAME}"
admin_password           = "${ADMIN_PASSWORD}"
production_mode_enabled  = true

admin_server_name        = "${ADMIN_SERVER_NAME}"
admin_server_port        = ${ADMIN_SERVER_PORT}
t3_channel_port          = ${T3_CHANNEL_PORT}
t3_public_address        = "${T3_PUBLIC_ADDRESS}"

cluster_name             = "${CLUSTER_NAME}"
number_of_ms             = ${MANAGED_SERVER_COUNT}
managed_server_port      = ${MANAGED_SERVER_PORT}
managed_server_base_name = "${MANAGED_SERVER_NAME_BASE}"

logs_dir                  = "${POD_DOMAIN_LOGS_DIR}"

selectTemplate('Basic WebLogic Server Domain', '12.2.1.3.0')
loadTemplates()
#readTemplate("${MW_HOME}/wlserver/common/templates/wls/wls.jar")

# configure the domain
cd('/')
cmo.setName(domain_name)
setOption('DomainName', domain_name)
log=create(domain_name, 'Log')
log.setFileName(logs_dir + '/' + domain_name + '.log')

cd('/Security/' + domain_name + '/User/weblogic')
cmo.setName(admin_username)
cmo.setPassword(admin_password)

cd('/NMProperties')
set('ListenAddress', '0.0.0.0')
set('ListenPort', 5556)
set('CrashRecoveryEnabled', 'true')
set('NativeVersionEnabled', 'true')
set('StartScriptEnabled', 'false')
set('SecureListener', 'false')
set('LogLevel', 'FINEST')
set('DomainsDirRemoteSharingEnabled', 'true')

cd('/SecurityConfiguration/base_domain')
cmo.setNodeManagerUsername(admin_username)
cmo.setNodeManagerPasswordEncrypted(admin_password)

# configure the admin server
cd('/Servers/AdminServer')
cmo.setListenAddress(domain_uid + '-' + admin_server_name)
cmo.setListenPort(admin_server_port)
cmo.setName(admin_server_name)

log=create(admin_server_name, 'Log')
log.setFileName(logs_dir + '/' + admin_server_name + '.log')

nap=create('T3Channel', 'NetworkAccessPoint')
nap.setPublicPort(t3_channel_port)
nap.setPublicAddress(t3_public_address)
nap.setListenAddress(domain_uid + '-' + admin_server_name)
nap.setListenPort(t3_channel_port)

# create the cluster
cd('/')
cl=create(cluster_name, 'Cluster')

template_name = cluster_name + "-template"
st=create(template_name, 'ServerTemplate')
st.setListenPort(managed_server_port)
st.setListenAddress(domain_uid + '-' + managed_server_base_name + '\${id}')
st.setCluster(cl)
cd('/ServerTemplates/' + template_name)
log=create(template_name, 'Log')
log.setFileName(logs_dir + '/' + managed_server_base_name + '\${id}.log')

cd('/Clusters/' + cluster_name)
ds=create(cluster_name, 'DynamicServers')
ds.setServerTemplate(st)
ds.setServerNamePrefix(managed_server_base_name)
ds.setDynamicClusterSize(number_of_ms)
ds.setMaxDynamicClusterSize(number_of_ms)
ds.setCalculatedListenPorts(false)
ds.setIgnoreSessionsDuringShutdown(true)
#cd('DynamicServers/' + cluster_name)
#set('Id', 1)

# write out the domain
setOption('OverwriteDomain', 'true')
writeDomain(domain_path)
closeTemplate()

# convert it to production mode
readDomain(domain_path)
cd('/')
cmo.setProductionModeEnabled(true)
updateDomain()
closeDomain()

exit()

EOF

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


