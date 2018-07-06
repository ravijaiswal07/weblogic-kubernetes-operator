#
# +++ Start of common code for reading domain secrets

# Read username secret
file = open('/weblogic-operator/secrets/username', 'r')
admin_username = file.read()
file.close()

# Read password secret
file = open('/weblogic-operator/secrets/password', 'r')
admin_password = file.read()
file.close()

# +++ End of common code for reading domain secrets
#
server_name = os.getenv('SERVER_NAME')
domain_uid = os.getenv('DOMAIN_UID')
domain_name = os.getenv('DOMAIN_NAME')
domain_home = os.getenv('DOMAIN_HOME')

service_name = domain_uid + "-" + server_name

# Connect to nodemanager and stop server
try:
  nmConnect(admin_username, admin_password, service_name,  '5556', domain_name, domain_home, 'plain')
except:
  print('Failed to connect to the NodeManager')
  exit(exitcode=2)

# Kill the server
try:
  nmKill(server_name)
except:
  print('Connected to the NodeManager, but failed to stop the server')
  exit(exitcode=2)

# Exit WLST
nmDisconnect()
exit()
