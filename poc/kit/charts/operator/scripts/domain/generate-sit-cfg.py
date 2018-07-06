#
# +++ Start of common utilities

def writeln(f, v):
  f.write(v + "\n")

# +++ End of common utilities
#

#
# +++ Start of situational configuration generation

def readSecret(path):
  file = open(path, 'r')
  secret = file.read()
  file.close()
  return secret

def getDomainLogs():
  domain_logs = os.getenv('DOMAIN_LOGS')
  if domain_logs == "null":
    return None
  else:
    return domain_logs

def readAndEncryptSecret(path):
  secret = readSecret(path)
  domain_home=os.getenv('DOMAIN_HOME')
  return encrypt(secret, domain_home)

def beginDomain(f):
  writeln(f, "    <?xml version='1.0' encoding='UTF-8'?>")
  writeln(f, "    <d:domain xmlns:d=\"http://xmlns.oracle.com/weblogic/domain\" xmlns:f=\"http://xmlns.oracle.com/weblogic/domain-fragment\" xmlns:s=\"http://xmlns.oracle.com/weblogic/situational-config\">")
  writeln(f, "      <s:expiration> 2020-07-16T19:20+01:00 </s:expiration>")
  admin_username = readSecret('/weblogic-operator/secrets/username')
  admin_password = readAndEncryptSecret('/weblogic-operator/secrets/password')
  writeln(f, "      <d:security-configuration>")
  writeln(f, "        <d:node-manager-user-name f:combine-mode=\"replace\">" + admin_username + "</d:node-manager-user-name>")
  writeln(f, "        <d:node-manager-password-encrypted f:combine-mode=\"replace\">" + admin_password + "</d:node-manager-password-encrypted>")
  writeln(f, "      </d:security-configuration>")
  domain_logs = getDomainLogs()
  if domain_logs is not None:
    domain_name=cmo.getName()
    writeln(f, "      <d:log f:combine-mode=\"replace\">")
    writeln(f, "        <d:file-name>" + domain_logs + "/" + domain_name + ".log</d:file-name>")
    writeln(f, "      </d:log>")

def endDomain(f):
  writeln(f, "    </d:domain>")

def customizeServer(f, domain_uid, admin_server_name, server):
  name=server.getName()
  writeln(f, "      <d:server>")
  writeln(f, "        <d:name>" + name + "</d:name>")
  domain_logs = getDomainLogs()
  if domain_logs is not None:
    writeln(f, "        <d:log f:combine-mode=\"replace\">")
    writeln(f, "          <d:file-name>" + domain_logs + "/" + name + ".log</d:file-name>")
    writeln(f, "        </d:log>")
  writeln(f, "        <d:listen-address f:combine-mode=\"replace\">" + domain_uid + "-" + name + "</d:listen-address>")
  if name == admin_server_name:
    # TBD - find the t3 channel, and if it exists, customize its listen address
    writeln(f, "        <d:network-access-point>")
    writeln(f, "          <d:name>T3Channel</d:name>") # TBD - needs to be discovered, v.s. depending on a fixed name
    writeln(f, "          <d:listen-address f:combine-mode=\"replace\">" + domain_uid + "-" + name + "</d:listen-address>")
    writeln(f, "        </d:network-access-point>")
  writeln(f, "      </d:server>")

def customizeServerTemplate(f, domain_uid, template):
  name=template.getName()
  server_name_prefix=template.getCluster().getDynamicServers().getServerNamePrefix()
  writeln(f, "      <d:server-template>")
  writeln(f, "        <d:name>" + name + "</d:name>")
  domain_logs = getDomainLogs()
  if domain_logs is not None:
    writeln(f, "        <d:log f:combine-mode=\"replace\">")
    writeln(f, "          <d:file-name>/domain-logs/" + server_name_prefix + "\${i}.log</d:file-name>")
    writeln(f, "        </d:log>")
  #writeln(f, "        <d:listen-address f:combine-mode=\"replace\">" + domain_uid + "-" + server_name_prefix + "\${i}</d:listen-address>")
  writeln(f, "      </d:server-template>")

def createSitCfg(f):
  domain_uid=os.getenv('DOMAIN_UID')
  cd('/')
  admin_server_name=cmo.getAdminServerName()
  beginDomain(f)
  for server in cmo.getServers():
    customizeServer(f, domain_uid, admin_server_name, server)
  for template in cmo.getServerTemplates():
    customizeServerTemplate(f, domain_uid, template)
  endDomain(f)

# +++ End of situational configuration generation
#

# +++ Start of overall config map generation

def addConfigMapHeader(f):
  domain_uid=os.getenv('DOMAIN_UID')
  sitcfg_name=os.getenv('SITCFG_NAME')
  domains_namespace=os.getenv('DOMAINS_NAMESPACE')
  domain_name=cmo.getName()
  writeln(f, "apiVersion: v1")
  writeln(f, "kind: ConfigMap")
  writeln(f, "metadata:")
  writeln(f, "  labels:")
  writeln(f, "    weblogic.createdByOperator: \"true\"")
  writeln(f, "    weblogic.domainUID: " + domain_uid)
  writeln(f, "    weblogic.resourceVersion: domain-v1")
  writeln(f, "  name: " + domain_uid + "-" + sitcfg_name + '-sitcfg-cm')
  writeln(f, "  namespace: " + domains_namespace)

def endConfigMap(f):
  undent()

def addFileHeaderToConfigMap(f, name):
  writeln(f, "  " + name + ": |")

def addSitCfgToConfigMap(f):
  addFileHeaderToConfigMap(f, "operator-situational-config.xml")
  createSitCfg(f)

def addFilesToConfigMap(f):
  writeln(f, "data:")
  addSitCfgToConfigMap(f)

def createConfigMap():
  config_map_path=sys.argv[1]
  f=open(config_map_path, 'w+')
  addConfigMapHeader(f)
  addFilesToConfigMap(f)
  f.close()

def main():
  domain_home=os.getenv('DOMAIN_HOME')
  readDomain(domain_home)
  createConfigMap()
  closeDomain()
  exit()

main()

