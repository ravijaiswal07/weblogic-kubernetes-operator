#
# +++ Start of common utilities

def writeln(f, v):
  f.write(v + "\n")

# +++ Start of common utilities
#


#
# +++ Start of domain validation

def validateDomain():
  # TBD - move the assertions below from creating topology here
  # and print errors to a file and return false if anything is wrong
  # TBD - should we write it to the topology file instead?
  # TBD - is it really just true/false, or do we print warnings and ignore?
  return True

# +++ End of domain validation
#

#
# +++ Start of topology generation

def addAdminServerTopology(f):
  admin_name=cmo.getAdminServerName()
  admin_server=None
  for server in cmo.getServers():
    if admin_name == server.getName():
      admin_server = server
      # TBD - assert that the admin server is not clustered
      break
  # TBD - assertion for admin_server is not None ?
  writeln(f, "      adminServer: \"" + admin_server.getName() + "\"")

def findDynamicClusterServerTemplate(f, cluster):
  # find the server template for this cluster
  server_template=None
  for template in cmo.getServerTemplates():
    if template.getCluster() is cluster:
      # assert that there is only server template for this cluster
      assert server_template is None
      server_template = template
  assert server_template is not None
  return server_template

def addDynamicClusterTopology(f, cluster):
  # make sure that there are no servers using this cluster
  for server in cmo.getServers():
    assert server.getCluster() is not cluster
  dyn_servers = cluster.getDynamicServers()
  assert dyn_servers.isCalculatedListenPorts() == False
  server_template = findDynamicClusterServerTemplate(f, cluster)
  writeln(f, "      - name: \"" + cluster.getName() + "\"")
  writeln(f, "        port: " + str(server_template.getListenPort()))
  writeln(f, "        maxServers: " + str(dyn_servers.getMaxDynamicClusterSize()))
  writeln(f, "        baseServerName: \"" + dyn_servers.getServerNamePrefix() + "\"")

def addDynamicClustersTopology(f):
  writeln(f, "      dynamicClusters:")
  for cluster in cmo.getClusters():
    if cluster.getDynamicServers() is not None:
      addDynamicClusterTopology(f, cluster)

# all the servers in the non-dynamic cluster must have the same port number
# TBD - can/should we support per-server port numbers?
def findNonDynamicClusterPort(f, cluster):
  cluster_port=None
  for server in cmo.getServers():
    if server.getCluster() is cluster:
      port=server.getListenPort()
      if cluster_port is None:
        cluster_port=port
      else:
        assert port == cluster_port
  assert cluster_port is not None # i.e. that we have at least one server in the cluster
  return cluster_port

def addClusteredServerTopology(f, server):
  writeln(f, "        - name: \"" + server.getName() + "\"")

def addNonDynamicClusterTopology(f, cluster):
  # make sure that there are no server templates using this cluster
  for template in cmo.getServerTemplates():
    assert template.getCluster() is not cluster
  writeln(f, "      - name: \"" + cluster.getName() + "\"")
  writeln(f, "        port: \"" + str(findNonDynamicClusterPort(f, cluster)))
  writeln(f, "        servers:")
  for server in cmo.getServers():
    if server.getCluster() is cluster:
      addClusteredServerTopology(f, server)

def addNonDynamicClustersTopology(f):
  writeln(f, "      nonDynamicClusters:")
  for cluster in cmo.getClusters():
    if cluster.getDynamicServers() is None:
      addNonDynamicClusterTopology(f, cluster)

def addNonClusteredServerTopology(f, server):
  writeln(f, "      - name: \"" + server.getName() + "\"")
  writeln(f, "        port: " + str(server.getListenPort()))

def addNonClusteredServersTopology(f):
  writeln(f, "      servers:")
  for server in cmo.getServers():
    if server.getCluster() is None:
      addNonClusteredServerTopology(f, server)

def addDomainTopology(f):
  writeln(f, "    domain")
  writeln(f, "      name: \"" + cmo.getName() + "\"")

def createTopology(f):
  cd('/')
  addDomainTopology(f)
  addAdminServerTopology(f)
  addDynamicClustersTopology(f)
  addNonDynamicClustersTopology(f)
  addNonClusteredServersTopology(f)

# +++ End of topology generation
#

# +++ Start of overall config map generation

def addConfigMapHeader(f):
  domain_uid=os.getenv('DOMAIN_UID')
  domains_namespace=os.getenv('DOMAINS_NAMESPACE')
  domain_name=cmo.getName()
  writeln(f, "apiVersion: v1")
  writeln(f, "kind: ConfigMap")
  writeln(f, "metadata:")
  writeln(f, "  labels:")
  writeln(f, "    weblogic.createdByOperator: \"true\"")
  writeln(f, "    weblogic.domainUID: " + domain_uid)
  writeln(f, "    weblogic.resourceVersion: domain-v1")
  writeln(f, "  name: " + domain_uid + "-weblogic-topology-cm")
  writeln(f, "  namespace: " + domains_namespace)

def endConfigMap(f):
  undent()

def addFileHeaderToConfigMap(f, name):
  writeln(f, "  " + name + ": |")

def addTopologyToConfigMap(f):
  addFileHeaderToConfigMap(f, "topology.yaml")
  createTopology(f)

def addFilesToConfigMap(f):
  writeln(f, "data:")
  addTopologyToConfigMap(f)

def createConfigMap():
  config_map_path=sys.argv[1]
  f=open(config_map_path, 'w+')
  addConfigMapHeader(f)
  addFilesToConfigMap(f)
  f.close()

def main():
  domain_home=os.getenv('DOMAIN_HOME')
  readDomain(domain_home)
  if validateDomain() == True:
    createConfigMap()
  closeDomain()
  exit()

main()

