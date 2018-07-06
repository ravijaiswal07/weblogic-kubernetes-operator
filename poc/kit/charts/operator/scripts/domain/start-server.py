import sys;
#
# +++ Start of common code for reading domain secrets

# Read username secret
file = open('/weblogic-operator/secrets/username', 'r')
admin_username = file.read()
file.close()
print('admin_username=' + admin_username)

# Read password secret
file = open('/weblogic-operator/secrets/password', 'r')
admin_password = file.read()
file.close()
print('admin_password=' + admin_password)

# +++ End of common code for reading domain secrets
#
server_name = os.getenv('SERVER_NAME')
domain_uid = os.getenv('DOMAIN_UID')
domain_name = os.getenv('DOMAIN_NAME')
domain_home = os.getenv('DOMAIN_HOME')
admin_name = os.getenv('ADMIN_NAME')
admin_port = os.getenv('ADMIN_PORT')

print 'admin username is %s' % admin_username
print 'admin password is %s' % admin_password
print 'domain home is %s' % domain_home
print 'server name is %s' % server_name
if server_name != admin_name:
  admin_server_url='t3://' + domain_uid + '-' + admin_name + ':' + admin_port
  print 'admin server url is %s' % admin_server_url

# Encrypt the admin username and password
adminUsernameEncrypted=encrypt(admin_username, domain_home)
adminPasswordEncrypted=encrypt(admin_password, domain_home)

print 'Create boot.properties files for this server'

# Define the folder path
secdir='%s/servers/%s/security' % (domain_home, server_name)

# Create the security folder (if it does not already exist)
try:
  os.makedirs(secdir)
except OSError:
  if not os.path.isdir(secdir):
    raise

print 'writing boot.properties to %s/servers/%s/security/boot.properties' % (domain_home, server_name)

bpFile=open('%s/servers/%s/security/boot.properties' % (domain_home, server_name), 'w+')
# bpFile.write("username=%s\n" % adminUsernameEncrypted)
# bpFile.write("password=%s\n" % adminPasswordEncrypted)
bpFile.write("username=%s\n" % admin_username)
bpFile.write("password=%s\n" % admin_password)
bpFile.close()

service_name = domain_uid + "-" + server_name

# Connect to nodemanager and start server
try:
  nmConnect(admin_username, admin_password, service_name,  '5556', domain_name, domain_home, 'plain')
  nmStart(server_name)
  nmDisconnect()
except WLSTException, e:
  nmDisconnect()
  print e

# Exit WLST
exit()
