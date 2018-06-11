#!/bin/bash

DOMAIN_HOME_DIR=${1}
OUTPUT_DIR=${2}

CONFIG_MAP=${OUTPUT_DIR}/domain-bindings-cm.yaml
rm -f ${CONFIG_MAP}

#set -x

java weblogic.WLST << EOF

indentLevel=0
indentSpaces=""

def setIndentSpaces():
  global indentSpaces
  indentSpaces=""
  for x in range(indentLevel):
    indentSpaces=indentSpaces+"  "

def indent():
  global indentLevel
  indentLevel=indentLevel+1
  setIndentSpaces()

def undent():
  global indentLevel
  indentLevel=indentLevel-1
  setIndentSpaces()


def writeln(os, v):
  global indentSpaces
  os.write(indentSpaces + v + "\n")

def validateDomain():
  # TBD - move the assertions below from creating topology here
  # and print errors to a file and return false if anything is wrong
  # TBD - should we write it to the topology file instead?
  # TBD - is it really just true/false, or do we print warnings and ignore?
  return True

def addAdminServerTopology(os):
  admin_name=cmo.getAdminServerName()
  admin_server=None
  for server in cmo.getServers():
    if admin_name == server.getName():
      admin_server = server
      # TBD - assert that the admin server is not clustered
      break
  # TBD - assertion for admin_server is not None ?
  writeln(os, "  adminServer: \"" + admin_server.getName() + "\"")

def findDynamicClusterServerTemplate(os, cluster):
  # find the server template for this cluster
  server_template=None
  for template in cmo.getServerTemplates():
    if template.getCluster() is cluster:
      # assert that there is only server template for this cluster
      assert server_template is None
      server_template = template
  assert server_template is not None
  return server_template

def addDynamicClusterTopology(os, cluster):
  # make sure that there are no servers using this cluster
  for server in cmo.getServers():
    assert server.getCluster() is not cluster
  dyn_servers = cluster.getDynamicServers()
  assert dyn_servers.isCalculatedListenPorts() == False
  server_template = findDynamicClusterServerTemplate(os, cluster)
  writeln(os, "  - name: \"" + cluster.getName() + "\"")
  writeln(os, "    port: " + str(server_template.getListenPort()))
  writeln(os, "    maxServers: " + str(dyn_servers.getMaxDynamicClusterSize()))
  writeln(os, "    baseServerName: \"" + dyn_servers.getServerNamePrefix() + "\"")

def addDynamicClustersTopology(os):
  writeln(os, "  dynamicClusters:")
  for cluster in cmo.getClusters():
    if cluster.getDynamicServers() is not None:
      addDynamicClusterTopology(os, cluster)

# all the servers in the non-dynamic cluster must have the same port number
# TBD - can/should we support per-server port numbers?
def findNonDynamicClusterPort(os, cluster):
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

def addClusteredServerTopology(os, server):
  writeln(os, "    - name: \"" + server.getName() + "\"")

def addNonDynamicClusterTopology(os, cluster):
  # make sure that there are no server templates using this cluster
  for template in cmo.getServerTemplates():
    assert template.getCluster() is not cluster
  writeln(os, "  - name: \"" + cluster.getName() + "\"")
  writeln(os, "    port: \"" + str(findNonDynamicClusterPort(os, cluster)))
  writeln(os, "    servers:")
  for server in cmo.getServers():
    if server.getCluster() is cluster:
      addClusteredServerTopology(os, server)

def addNonDynamicClustersTopology(os):
  writeln(os, "  nonDynamicClusters:")
  for cluster in cmo.getClusters():
    if cluster.getDynamicServers() is None:
      addNonDynamicClusterTopology(os, cluster)

def addNonClusteredServerTopology(os, server):
  writeln(os, "  - name: \"" + server.getName() + "\"")
  writeln(os, "    port: " + str(server.getListenPort()))

def addNonClusteredServersTopology(os):
  writeln(os, "  servers:")
  for server in cmo.getServers():
    if server.getCluster() is None:
      addNonClusteredServerTopology(os, server)

def addDomainTopology(os):
  writeln(os, "domain")
  writeln(os, "  name: \"" + cmo.getName() + "\"")

def createTopology(os):
  cd('/')
  addDomainTopology(os)
  addAdminServerTopology(os)
  addDynamicClustersTopology(os)
  addNonDynamicClustersTopology(os)
  addNonClusteredServersTopology(os)

def beginDomain(os):
  domain_name=cmo.getName()
  writeln(os, "<?xml version='1.0' encoding='UTF-8'?>")
  writeln(os, "<d:domain xmlns:d=\"http://xmlns.oracle.com/weblogic/domain\" xmlns:f=\"http://xmlns.oracle.com/weblogic/domain-fragment\" xmlns:s=\"http://xmlns.oracle.com/weblogic/situational-config\">")
  writeln(os, "  <s:expiration> 2020-07-16T19:20+01:00 </s:expiration>")
  writeln(os, "  <d:log f:combine-mode=\"replace\">")
  writeln(os, "    <d:file-name>/domain-logs/" + domain_name + ".log</d:file-name>")
  writeln(os, "  </d:log>")

def endDomain(os):
  writeln(os, "</d:domain>")

def customizeServer(os, domain_uid, admin_server_name, server):
  name=server.getName()
  writeln(os, "  <d:server>")
  writeln(os, "    <d:name>" + name + "</d:name>")
  writeln(os, "    <d:log f:combine-mode=\"replace\">")
  writeln(os, "      <d:file-name>/domain-logs/" + name + ".log</d:file-name>")
  writeln(os, "    </d:log>")
  writeln(os, "    <d:listen-address f:combine-mode=\"replace\">" + domain_uid + "-" + name + "</d:listen-address>")
  if name == admin_server_name:
    # TBD - find the t3 channel, and if it exists, customize its listen address
    writeln(os, "    <d:network-access-point>")
    writeln(os, "      <d:name>T3Channel</d:name>") # TBD - needs to be discovered, v.s. depending on a fixed name
    writeln(os, "      <d:listen-address f:combine-mode=\"replace\">" + domain_uid + "-" + name + "</d:listen-address>")
    writeln(os, "    </d:network-access-point>")
  writeln(os, "  </d:server>")

def customizeServerTemplate(os, domain_uid, template):
  name=template.getName()
  server_name_prefix=template.getCluster().getDynamicServers().getServerNamePrefix()
  writeln(os, "  <d:server-template>")
  writeln(os, "    <d:name>" + name + "</d:name>")
  writeln(os, "    <d:log f:combine-mode=\"replace\">")
  writeln(os, "      <d:file-name>/domain-logs/" + server_name_prefix + "\${i}.log</d:file-name>")
  writeln(os, "    </d:log>")
  #writeln(os, "    <d:listen-address f:combine-mode=\"replace\">" + domain_uid + "-" + server_name_prefix + "\${i}</d:listen-address>")
  writeln(os, "  </d:server-template>")

def createSitConfig(os):
  domain_uid="${DOMAIN_UID}"
  cd('/')
  admin_server_name=cmo.getAdminServerName()
  beginDomain(os)
  for server in cmo.getServers():
    customizeServer(os, domain_uid, admin_server_name, server)
  for template in cmo.getServerTemplates():
    customizeServerTemplate(os, domain_uid, template)
  endDomain(os)

def addConfigMapHeader(os):
  domain_uid="${DOMAIN_UID}"
  domain_name=cmo.getName()
  writeln(os, "apiVersion: v1")
  writeln(os, "kind: ConfigMap")
  writeln(os, "metadata:")
  writeln(os, "  labels:")
  writeln(os, "    weblogic.createdByOperator: \"true\"")
  writeln(os, "    weblogic.domainName: " + domain_name)
  writeln(os, "    weblogic.domainUID: " + domain_uid)
  writeln(os, "    weblogic.resourceVersion: domain-v1")
  writeln(os, "  name: " + domain_uid + "-weblogic-domain-bindings-cm")
  writeln(os, "  namespace: ${DOMAIN_NAMESPACE}")

def endConfigMap(os):
  undent()

def addFileHeaderToConfigMap(os, name):
  writeln(os, name + ": |")

def addTopologyToConfigMap(os):
  addFileHeaderToConfigMap(os, "topology.yaml")
  indent()
  createTopology(os)
  undent()

def addSitConfigToConfigMap(os):
  addFileHeaderToConfigMap(os, "operator-situational-config.xml")
  indent()
  createSitConfig(os)
  undent()

def addFilesToConfigMap(os):
  writeln(os, "data:")
  indent()
  addTopologyToConfigMap(os)
  addSitConfigToConfigMap(os)
  undent()

def createConfigMap():
  config_map_path="${CONFIG_MAP}"
  os=open(config_map_path, 'w+')
  addConfigMapHeader(os)
  addFilesToConfigMap(os)
  os.close()

def main():
  domain_home="${DOMAIN_HOME_DIR}"
  readDomain(domain_home)
  if validateDomain() == True:
    createConfigMap()
  closeDomain()
  exit()

main()

EOF
