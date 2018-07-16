import sys;

server_name = os.getenv('SERVER_NAME')
domain_uid = os.getenv('DOMAIN_UID')
domain_name = os.getenv('DOMAIN_NAME')
domain_home = os.getenv('DOMAIN_HOME')
admin_name = os.getenv('ADMIN_NAME')
admin_port = os.getenv('ADMIN_PORT')

print 'domain home is %s' % domain_home
print 'server name is %s' % server_name
if server_name != admin_name:
  admin_server_url='t3://' + domain_uid + '-' + admin_name + ':' + admin_port
  print 'admin server url is %s' % admin_server_url

service_name = domain_uid + "-" + server_name
user_config = "/weblogic-operator/server/cm/userConfig"
user_key = "/weblogic-operator/server/secret/userKey"

# Connect to nodemanager and start server
try:
  nmConnect(userConfigFile=user_config, userKeyFile=user_key, host=service_name, port='5556', domainName=domain_name, domainDir=domain_home, nmType = "plain")
  nmStart(server_name)
  nmDisconnect()
except WLSTException, e:
  nmDisconnect()
  print e

# Exit WLST
exit()
