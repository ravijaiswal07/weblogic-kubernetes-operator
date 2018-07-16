import base64
import sys
import traceback

class OfflineWlstEnv(object):

  def open(self):
    readDomain(self.getDomainHome())
    self.domain = cmo

  def close(self):
    closeDomain()

  def getDomain(self):
    return self.domain

  def getTopologyYamlPath(self):
    return self.getEnv('TOPOLOGY_YAML')

  def getServerConfigMapYamlPath(self):
    return self.getEnv('SERVER_CM_YAML')

  def getServerSecretYamlPath(self):
    return self.getEnv('SERVER_SECRET_YAML')

  def getDomainsNamespace(self):
    return self.getEnv('DOMAINS_NAMESPACE')

  def getDomainUID(self):
    return self.getEnv('DOMAIN_UID')

  def getTemplateName(self):
    return self.getEnv('TEMPLATE_NAME')

  def getDomainHome(self):
    return self.getEnv('DOMAIN_HOME')

  def getDomainLogs(self):
    return self.getEnv('DOMAIN_LOGS')

  def getSecretsDir(self):
    return self.getEnv('SECRETS_DIR')

  def getEnv(self, name):
    val = os.getenv(name)
    if val == "null":
      return None
    else:
      return val

  def createUserConfigAndKey(self, username, password):
    nm_host = self.getDomainUID() + "-domain-introspector" # i.e. the pod name.  TBD - pass in via the env?
    nm_port = '5556' # TBD - pass in via the env?
    print "nmConnect " + username + ", " + password + ", " + nm_host + ", " + nm_port + ", " + self.getDomain().getName() + ", " + self.getDomainHome()
    nmConnect(username, password, nm_host, nm_port, self.getDomain().getName(), self.getDomainHome(), 'plain')
    userConfigFile = "/u01/userConfigNodeManager.secure"
    userKeyFile = "/u01/userKeyNodeManager.secure"
    isNodeManager = "true"
    try:
      storeUserConfig(userConfigFile, userKeyFile, isNodeManager)
    finally:
      nmDisconnect()
    self.userConfig = self.readFile(userConfigFile)
    self.userKey = self.readBinaryFile(userKeyFile)

  def getUserConfig(self):
    return self.userConfig

  def getUserKey(self):
    return self.userKey

  def encrypt(self, cleartext):
    return encrypt(cleartext, self.getDomainHome())

  def readFile(self, path):
    file = open(path, 'r')
    contents = file.read()
    file.close()
    return contents

  def readBinaryFile(self, path):
    file = open(path, 'rb')
    contents = file.read()
    file.close()
    return contents

class SecretManager(object):

  def __init__(self, env):
    self.env = env

  def readAndEncryptSecret(self, name):
    cleartext = self.readSecret(name)
    return self.env.encrypt(cleartext)

  def readSecret(self, name):
    path = self.env.getSecretsDir() + "/" + name
    file = open(path, 'r')
    cleartext = file.read()
    file.close()
    return cleartext

class Generator(SecretManager):

  def __init__(self, env, path):
    SecretManager.__init__(self, env)
    self.env = env
    self.path = path
    self.indentStack = [""]

  def open(self):
    self.f =  open(self.path, 'w+')

  def close(self):
    self.f.close()

  def indent(self):
    self.indentStack.append(self.indentPrefix() + "  ")

  def undent(self):
    self.indentStack.pop()

  def indentPrefix(self):
    return self.indentStack[len(self.indentStack)-1]

  def writeln(self, msg):
    self.f.write(self.indentPrefix() + msg + "\n")

  def quote(self, val):
    return "\"" + val + "\""

  def name(self, mbean):
    return "\"" + mbean.getName() + "\"";

class TopologyGenerator(Generator):

  def __init__(self, env):
    Generator.__init__(self, env, env.getTopologyYamlPath())
    self.errors = []

  def validate(self):
    self.validateAdminServer()
    self.validateClusters()
    return self.isValid()

  def generate(self):
    self.open()
    try:
      if self.isValid():
        self.generateTopology()
      else:
        self.reportErrors()
    finally:
      self.close()

  def validateAdminServer(self):
    adminServerName = self.env.getDomain().getAdminServerName()
    if adminServerName == None:
      addError("The admin server name is null.")
      return
    adminServer = None
    for server in self.env.getDomain().getServers():
      if adminServerName == server.getName():
        adminServer = server
    if adminServer is None:
      addError("The admin server '" + adminServerName + "' does not exist.")
      return
    cluster = adminServer.getCluster()
    if cluster != None:
      self.addError("The admin server " + self.name(adminServer) + " belongs to the cluster " + self.name(cluster) + ".")

  def validateClusters(self):
    for cluster in self.env.getDomain().getClusters():
      self.validateCluster(cluster)

  def validateCluster(self, cluster):
    if cluster.getDynamicServers() is None:
      self.validateNonDynamicCluster(cluster)
    else:
      self.validateDynamicCluster(cluster)

  def validateNonDynamicCluster(self, cluster):
    self.validateNonDynamicClusterReferencedByAtLeastOneServer(cluster)
    self.validateNonDynamicClusterNotReferencedByAnyServerTemplates(cluster)
    self.validateNonDynamicClusterServersHaveSameListenPort(cluster)

  def validateNonDynamicClusterReferencedByAtLeastOneServer(self, cluster):
    for server in self.env.getDomain().getServers():
      if server.getCluster() is cluster:
        return
    self.addError("The non-dynamic cluster " + self.name(cluster) + " is not referenced by any servers.")

  def validateNonDynamicClusterNotReferencedByAnyServerTemplates(self, cluster):
    for template in self.env.getDomain().getServerTemplates():
      if template.getCluster() is cluster:
        self.addError("The non-dynamic cluster " + self.name(cluster) + " is referenced by the server template " + self.name(template) + ".")

  def validateNonDynamicClusterServersHaveSameListenPort(self, cluster):
    firstServer = None
    firstPort = None
    for server in self.env.getDomain().getServers():
      if cluster is server.getCluster():
        port = server.getListenPort()
        if firstServer is None:
          firstServer = server
          firstPort = port
        else:
          if port != firstPort:
            self.addError("The non-dynamic cluster " + self.name(cluster) + "'s server " + self.name(firstServer) + "'s listen port is " + str(firstPort) + " but its server " + self.name(server) + "'s listen port is " + str(port) + ".")
            return

  def validateDynamicCluster(self, cluster):
    self.validateDynamicClusterReferencedByOneServerTemplate(cluster)
    self.validateDynamicClusterDynamicServersDoNotUseCalculatedListenPorts(cluster)
    self.validateDynamicClusterNotReferencedByAnyServers(cluster)

  def validateDynamicClusterReferencedByOneServerTemplate(self, cluster):
    server_template=None
    for template in self.env.getDomain().getServerTemplates():
      if template.getCluster() is cluster:
        if server_template is None:
          server_template = template
        else:
          if server_template is not None:
            self.addError("The dynamic cluster " + self.name(cluster) + " is referenced the server template " + self.name(server_template) + " and the server template " + self.name(template) + ".")
            return
    if server_template is None:
      self.addError("The dynamic cluster " + self.name(cluster) + "' is not referenced by any server template.")

  def validateDynamicClusterNotReferencedByAnyServers(self, cluster):
    for server in self.env.getDomain().getServers():
      if server.getCluster() is cluster:
        self.addError("The dynamic cluster " + self.name(cluster) + " is referenced by the server " + self.name(server) + ".")

  def validateDynamicClusterDynamicServersDoNotUseCalculatedListenPorts(self, cluster):
    if cluster.getDynamicServers().isCalculatedListenPorts() == True:
      self.addError("The dynamic cluster " + self.name(cluster) + "'s dynamic servers use calculated listen ports.")

  def isValid(self):
    return len(self.errors) == 0

  def addError(self, error):
    self.errors.append(error)

  def reportErrors(self):
    self.writeln("domainValid: false")
    self.writeln("validationErrors:")
    for error in self.errors:
      self.writeln("- \"" + error.replace("\"", "\\\"") + "\"")

  def generateTopology(self):
    self.writeln("domainValid: true")
    self.addDomain()

  def addDomain(self):
    self.writeln("domain:")
    self.indent()
    self.writeln("name: " + self.name(self.env.getDomain()))
    self.writeln("adminServerName: " + self.quote(self.env.getDomain().getAdminServerName()))
    self.addConfiguredClusters()
    self.addDynamicClusters()
    self.addNonClusteredServers()
    self.undent()

  def addConfiguredClusters(self):
    clusters = self.getConfiguredClusters()
    if len(clusters) == 0:
      self.writeln("configuredlusters: {}")
      return
    self.writeln("configuredClusters:")
    self.indent()
    for cluster in clusters:
      self.addConfiguredCluster(cluster)
    self.undent()
  
  def getConfiguredClusters(self):
    rtn = []
    for cluster in self.env.getDomain().getClusters():
      if cluster.getDynamicServers() is None:
        rtn.append(cluster)
    return rtn

  def addConfiguredCluster(self, cluster):
    self.writeln(self.name(cluster) + ":")
    self.indent()
    servers = self.getClusteredServers(cluster)
    self.writeln("port: " + str(servers[0].getListenPort()))
    self.writeln("servers:")
    self.indent()
    for server in servers:
      self.addClusteredServer(cluster, server)
    self.undent()
    self.undent()

  def getClusteredServers(self, cluster):
    rtn = []
    for server in self.env.getDomain().getServers():
      if server.getCluster() is cluster:
        rtn.append(server)
    return rtn

  def addClusteredServer(self, cluster, server):
    self.writeln(self.name(server) + ": {}")

  def addDynamicClusters(self):
    clusters = self.getDynamicClusters()
    if len(clusters) == 0:
      self.writeln("dynamicClusters: {}")
      return
    self.writeln("dynamicClusters:")
    self.indent()
    for cluster in clusters:
      self.addDynamicCluster(cluster)
    self.undent()
  
  def getDynamicClusters(self):
    rtn = []
    for cluster in self.env.getDomain().getClusters():
      if cluster.getDynamicServers() is not None:
        rtn.append(cluster)
    return rtn

  def addDynamicCluster(self, cluster):
    self.writeln(self.name(cluster) + ":")
    self.indent()
    template = self.findDynamicClusterServerTemplate(cluster)
    dyn_servers = cluster.getDynamicServers()
    self.writeln("port: " + str(template.getListenPort()))
    self.writeln("maxServers: " + str(dyn_servers.getDynamicClusterSize()))
    self.writeln("baseServerName: " + self.quote(dyn_servers.getServerNamePrefix()))
    self.undent()

  def findDynamicClusterServerTemplate(self, cluster):
    for template in cmo.getServerTemplates():
      if template.getCluster() is cluster:
        return template
    # should never get here - the domain validator already checked that
    # one server template references the cluster
    return None

  def addNonClusteredServers(self):
    # the domain validator already checked that we have a non-clustered admin server
    # therefore we know there will be at least one non-clustered server
    self.writeln("servers:")
    self.indent()
    for server in self.env.getDomain().getServers():
      if server.getCluster() is None:
        self.addNonClusteredServer(server)
    self.undent()

  def addNonClusteredServer(self, server):
    self.writeln(self.name(server) + ":")
    self.indent()
    self.writeln("port: " + str(server.getListenPort()))
    self.undent()

class ServerConfigMapGenerator(Generator):

  def __init__(self, env):
    Generator.__init__(self, env, env.getServerConfigMapYamlPath())

  def generate(self):
    self.open()
    try:
      self.addConfigMap()
    finally:
      self.close()

  def addConfigMap(self):
    self.writeln("apiVersion: \"v1\"")
    self.writeln("kind: \"ConfigMap\"")
    self.addMetadata()
    self.addData()

  def addMetadata(self):
    domain_uid = self.env.getDomainUID()
    self.writeln("metadata:")
    self.indent()
    self.writeln("name: " + self.quote(domain_uid + "-" + self.env.getTemplateName() + "-server-cm"))
    self.writeln("namespace: " + self.quote(self.env.getDomainsNamespace()))
    self.writeln("labels:")
    self.indent()
    self.writeln("weblogic.createdByOperator: \"true\"")
    self.writeln("weblogic.domainUID: " + self.quote(domain_uid))
    self.writeln("weblogic.resourceVersion: \"domain-v1\"")
    self.undent()
    self.undent()

  def addData(self):
    self.writeln("data:")
    self.indent()
    self.addBootProperties()
    self.addUserConfig()
    self.addSitCfg()
    self.undent()

  def addBootProperties(self):
    self.writeln("boot.properties: |")
    self.indent()
    self.writeln("username=" + self.readAndEncryptSecret("username"))
    self.writeln("password=" + self.readAndEncryptSecret("password"))
    self.undent()

  def addUserConfig(self):
    self.writeln("userConfig: |")
    self.indent()
    for s in self.env.getUserConfig().splitlines():
      self.writeln(s)
    self.undent()

  def addSitCfg(self):
    self.writeln("operator-situational-config.xml: |")
    self.indent()
    self.addSitCfgXml()
    self.undent()

  def addSitCfgXml(self):
    self.writeln("<?xml version='1.0' encoding='UTF-8'?>")
    self.writeln("<d:domain xmlns:d=\"http://xmlns.oracle.com/weblogic/domain\" xmlns:f=\"http://xmlns.oracle.com/weblogic/domain-fragment\" xmlns:s=\"http://xmlns.oracle.com/weblogic/situational-config\">")
    self.indent()
    self.writeln("<s:expiration> 2020-07-16T19:20+01:00 </s:expiration>")
    self.customizeNodeManagerCreds()
    self.customizeDomainLogPath()
    self.customizeServers()
    self.customizeServerTemplates()
    self.undent()
    self.writeln("</d:domain>")

  def customizeNodeManagerCreds(self):
    admin_username = self.readSecret('username')
    admin_password = self.readAndEncryptSecret('password')
    self.writeln("<d:security-configuration>")
    self.indent()
    self.writeln("<d:node-manager-user-name f:combine-mode=\"replace\">" + admin_username + "</d:node-manager-user-name>")
    self.writeln("<d:node-manager-password-encrypted f:combine-mode=\"replace\">" + admin_password + "</d:node-manager-password-encrypted>")
    self.undent()
    self.writeln("</d:security-configuration>")

  def customizeDomainLogPath(self):
    self.customizeLog(self.env.getDomain().getName())

  def customizeServers(self):
    for server in self.env.getDomain().getServers():
      self.customizeServer(server)

  def customizeServer(self, server):
    name=server.getName()
    self.writeln("<d:server>")
    self.indent()
    self.writeln("<d:name>" + name + "</d:name>")
    self.writeln("<d:listen-address f:combine-mode=\"replace\">" + self.env.getDomainUID() + "-" + name + "</d:listen-address>")
    self.customizeLog(name)
    self.undent()
    self.writeln("</d:server>")

  def customizeServerTemplates(self):
    for template in self.env.getDomain().getServerTemplates():
      self.customizeServerTemplate(template)

  def customizeServerTemplate(self, template):
    name=template.getName()
    server_name_prefix=template.getCluster().getDynamicServers().getServerNamePrefix()
    self.writeln("<d:server-template>")
    self.indent()
    self.writeln("<d:name>" + name + "</d:name>")
    #self.writeln("<d:listen-address f:combine-mode=\"replace\">" + self.env.getDomainUID() + "-" + server_name_prefix + "\${i}</d:listen-address>")
    self.customizeLog(server_name_prefix + "\${i}.log")
    self.undent()
    self.writeln("</d:server-template>")

  def customizeLog(self, name):
    logs_dir = self.env.getDomainLogs()
    if logs_dir is not None:
      self.writeln("<d:log f:combine-mode=\"replace\">")
      self.indent()
      self.writeln("<d:file-name>" + logs_dir + "/" + name + ".log</d:file-name>")
      self.undent()
      self.writeln("</d:log>")

class ServerSecretGenerator(Generator):

  def __init__(self, env):
    Generator.__init__(self, env, env.getServerSecretYamlPath())

  def generate(self):
    self.open()
    try:
      self.addSecret()
    finally:
      self.close()

  def addSecret(self):
    self.writeln("apiVersion: \"v1\"")
    self.writeln("kind: \"Secret\"")
    self.writeln("type: \"Opaque\"")
    self.addMetadata()
    self.addData()

  def addMetadata(self):
    domain_uid = self.env.getDomainUID()
    self.writeln("metadata:")
    self.indent()
    self.writeln("name: " + self.quote(domain_uid + "-" + self.env.getTemplateName() + "-server-secret"))
    self.writeln("namespace: " + self.quote(self.env.getDomainsNamespace()))
    self.writeln("labels:")
    self.indent()
    self.writeln("weblogic.createdByOperator: \"true\"")
    self.writeln("weblogic.domainUID: " + self.quote(domain_uid))
    self.writeln("weblogic.resourceVersion: \"domain-v1\"")
    self.undent()
    self.undent()

  def addData(self):
    self.writeln("data:")
    self.indent()
    self.addUserKey()
    self.undent()

  def addUserKey(self):
    # base64.encodestring can split the encoding over multiple lines.
    # kubernetes secrets need it on one line.
    # so, split it and merge it.
    b64 = ""
    for s in base64.encodestring(self.env.getUserKey()).splitlines():
      b64 = b64 + s
    self.writeln("userKey: " + self.quote(b64))

class DomainIntrospector(SecretManager):

  def __init__(self, env):
    SecretManager.__init__(self, env)
    self.env = env

  def introspect(self):
    tg = TopologyGenerator(self.env)
    if tg.validate():
      self.createUserConfigAndKey()
      ServerConfigMapGenerator(self.env).generate()
      ServerSecretGenerator(self.env).generate()
    # create the domain topology file last since the readiness and liveness
    # probes assume that the introspection is done if the file exists
    tg.generate()

  def createUserConfigAndKey(self):
    admin_username = self.readSecret("username")
    admin_password = self.readSecret("password")
    self.env.createUserConfigAndKey(admin_username, admin_password)

def main(env):
  try:
    env.open()
    try:
      DomainIntrospector(env).introspect()
    finally:
      env.close()
    exit(exitcode=0)
  except:
    print "Domain introspection unexpectedly failed:"
    traceback.print_exc()
    exit(exitcode=1)

main(OfflineWlstEnv())
