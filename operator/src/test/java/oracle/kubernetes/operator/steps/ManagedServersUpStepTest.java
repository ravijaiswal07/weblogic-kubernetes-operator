package oracle.kubernetes.operator.steps;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import io.kubernetes.client.models.V1EnvVar;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import oracle.kubernetes.operator.helpers.ClusterConfig;
import oracle.kubernetes.operator.helpers.ClusteredServerConfig;
import oracle.kubernetes.operator.helpers.DomainConfig;
import oracle.kubernetes.operator.helpers.DomainPresenceInfo;
import oracle.kubernetes.operator.helpers.NonClusteredServerConfig;
import oracle.kubernetes.operator.wlsconfig.WlsClusterConfig;
import oracle.kubernetes.operator.wlsconfig.WlsDomainConfig;
import org.junit.Test;

public class ManagedServersUpStepTest {
  private static final String CLUSTER1 = "cluster-1";
  private static final String SERVER0 = "ms-0";
  private static final String SERVER1 = "ms-1";
  private static final String SERVER2 = "ms-2";
  private static final String SERVER3 = "ms-3";
  private static final String SERVER4 = "ms-4";
  private static final String AS_NAME = "admin-server";

  private final String JSON_STRING_1_CLUSTER =
      "{     \"name\": \"base_domain\",\n "
          + "\"servers\": {\"items\": [\n"
          + "    {\n"
          + "        \"listenAddress\": \"\",\n"
          + "        \"name\": \"admin-server\",\n"
          + "        \"listenPort\": 8001,\n"
          + "        \"cluster\": null,\n"
          + "        \"networkAccessPoints\": {\"items\": []}\n"
          + "    },\n"
          + "    {\n"
          + "        \"listenAddress\": \"ms-0.wls-subdomain.default.svc.cluster.local\",\n"
          + "        \"name\": \"ms-0\",\n"
          + "        \"listenPort\": 8011,\n"
          + "        \"cluster\": [\n"
          + "            \"clusters\",\n"
          + "            \"cluster-1\"\n"
          + "        ],\n"
          + "        \"networkAccessPoints\": {\"items\": [\n"
          + "            {\n"
          + "                \"protocol\": \"t3\",\n"
          + "                \"name\": \"Channel-0\",\n"
          + "                \"listenPort\": 8012\n"
          + "            },\n"
          + "            {\n"
          + "                \"protocol\": \"t3\",\n"
          + "                \"name\": \"Channel-1\",\n"
          + "                \"listenPort\": 8013\n"
          + "            },\n"
          + "            {\n"
          + "                \"protocol\": \"t3s\",\n"
          + "                \"name\": \"Channel-2\",\n"
          + "                \"listenPort\": 8014\n"
          + "            }\n"
          + "        ]},\n"
          + "            \"SSL\": {\n"
          + "                \"enabled\": true,\n"
          + "                \"listenPort\": 8101\n"
          + "            }\n"
          + "    },\n"
          + "    {\n"
          + "        \"listenAddress\": \"ms-1.wls-subdomain.default.svc.cluster.local\",\n"
          + "        \"name\": \"ms-1\",\n"
          + "        \"listenPort\": 8011,\n"
          + "        \"cluster\": [\n"
          + "            \"clusters\",\n"
          + "            \"cluster-1\"\n"
          + "        ],\n"
          + "        \"networkAccessPoints\": {\"items\": []}\n"
          + "    },\n"
          + "    {\n"
          + "        \"listenAddress\": \"ms-2.wls-subdomain.default.svc.cluster.local\",\n"
          + "        \"name\": \"ms-2\",\n"
          + "        \"listenPort\": 8011,\n"
          + "        \"cluster\": [\n"
          + "            \"clusters\",\n"
          + "            \"cluster-1\"\n"
          + "        ],\n"
          + "        \"networkAccessPoints\": {\"items\": []}\n"
          + "    },\n"
          + "    {\n"
          + "        \"listenAddress\": \"ms-3.wls-subdomain.default.svc.cluster.local\",\n"
          + "        \"name\": \"ms-3\",\n"
          + "        \"listenPort\": 8011,\n"
          + "        \"cluster\": null,\n"
          + "        \"networkAccessPoints\": {\"items\": []}\n"
          + "    },\n"
          + "    {\n"
          + "        \"listenAddress\": \"ms-4.wls-subdomain.default.svc.cluster.local\",\n"
          + "        \"name\": \"ms-4\",\n"
          + "        \"listenPort\": 8011,\n"
          + "        \"cluster\": null,\n"
          + "        \"networkAccessPoints\": {\"items\": []}\n"
          + "    }\n"
          + "  ]}, "
          + "    \"machines\": {\"items\": [\n"
          + "        {\n"
          + "            \"name\": \"domain1-machine1\",\n"
          + "            \"nodeManager\": {\n"
          + "                \"NMType\": \"Plain\",\n"
          + "                \"listenAddress\": \"domain1-managed-server1\",\n"
          + "                \"name\": \"domain1-machine1\",\n"
          + "                \"listenPort\": 5556\n"
          + "            }\n"
          + "        },\n"
          + "        {\n"
          + "            \"name\": \"domain1-machine2\",\n"
          + "            \"nodeManager\": {\n"
          + "                \"NMType\": \"SSL\",\n"
          + "                \"listenAddress\": \"domain1-managed-server2\",\n"
          + "                \"name\": \"domain1-machine2\",\n"
          + "                \"listenPort\": 5556\n"
          + "            }\n"
          + "        }\n"
          + "    ]}\n"
          + "}";

  @Test
  public void processClusters() {
    WlsDomainConfig wlsDomainConfig = createWLSDomainConfig(JSON_STRING_1_CLUSTER);

    DomainConfig domainConfig = createDomainConfig(3);

    Collection<DomainPresenceInfo.ServerStartupInfo> ssic =
        new ArrayList<DomainPresenceInfo.ServerStartupInfo>();
    Collection<String> servers = new ArrayList<String>();
    Collection<String> clusters = new ArrayList<String>();

    ManagedServersUpStep.processClusters(
        wlsDomainConfig, domainConfig, ssic, AS_NAME, servers, clusters);
    System.out.println("ssic: " + ssic);
    assertEquals(3, ssic.size());
    System.out.println("servers: " + servers);
    assertEquals(3, servers.size());
    assertTrue(servers.contains(SERVER0));
    assertTrue(servers.contains(SERVER1));
    assertTrue(servers.contains(SERVER2));
    System.out.println("clusters: " + clusters);
    assertEquals(1, clusters.size());
    assertTrue(clusters.contains(CLUSTER1));
  }

  @Test
  public void processNonClusteredServers() {
    WlsDomainConfig wlsDomainConfig = createWLSDomainConfig(JSON_STRING_1_CLUSTER);

    DomainConfig domainConfig = createDomainConfig(2);

    Collection<DomainPresenceInfo.ServerStartupInfo> ssic =
        new ArrayList<DomainPresenceInfo.ServerStartupInfo>();
    Collection<String> servers = new ArrayList<String>();

    ManagedServersUpStep.processNonClusteredServers(
        wlsDomainConfig, domainConfig, ssic, AS_NAME, servers);
    System.out.println("ssic: " + ssic);
    assertEquals(2, ssic.size());
    System.out.println("servers: " + servers);
    assertEquals(2, servers.size());
    assertTrue(servers.contains(SERVER3));
    assertTrue(servers.contains(SERVER4));
  }

  @Test
  public void addClusteredServer_isAdminServer() {
    WlsDomainConfig wlsDomainConfig = createWLSDomainConfig(JSON_STRING_1_CLUSTER);
    WlsClusterConfig wlsClusterConfig = wlsDomainConfig.getClusterConfig(CLUSTER1);
    DomainConfig domainConfig = createDomainConfig(3);
    ClusterConfig clusterConfig = domainConfig.getClusters().get(CLUSTER1);
    ClusteredServerConfig clusteredServerConfig = clusterConfig.getServers().get(SERVER0);

    Collection<DomainPresenceInfo.ServerStartupInfo> ssic =
        new ArrayList<DomainPresenceInfo.ServerStartupInfo>();
    Collection<String> servers = new ArrayList<String>();

    int startedCount =
        ManagedServersUpStep.addClusteredServer(
            wlsDomainConfig,
            ssic,
            AS_NAME,
            servers,
            wlsClusterConfig,
            0,
            AS_NAME,
            clusteredServerConfig);
    assertEquals(0, startedCount);
  }

  @Test
  public void addClusteredServer_containsServerName() {
    WlsDomainConfig wlsDomainConfig = createWLSDomainConfig(JSON_STRING_1_CLUSTER);
    WlsClusterConfig wlsClusterConfig = wlsDomainConfig.getClusterConfig(CLUSTER1);
    DomainConfig domainConfig = createDomainConfig(3);
    ClusterConfig clusterConfig = domainConfig.getClusters().get(CLUSTER1);
    ClusteredServerConfig clusteredServerConfig = clusterConfig.getServers().get(SERVER0);

    Collection<DomainPresenceInfo.ServerStartupInfo> ssic =
        new ArrayList<DomainPresenceInfo.ServerStartupInfo>();
    Collection<String> servers = new ArrayList<String>();
    servers.add(SERVER0);
    servers.add(SERVER1);

    int startedCount =
        ManagedServersUpStep.addClusteredServer(
            wlsDomainConfig,
            ssic,
            AS_NAME,
            servers,
            wlsClusterConfig,
            0,
            SERVER0,
            clusteredServerConfig);
    assertEquals(0, startedCount);
  }

  @Test
  public void addClusteredServer_startAdminMode() {
    WlsDomainConfig wlsDomainConfig = createWLSDomainConfig(JSON_STRING_1_CLUSTER);
    WlsClusterConfig wlsClusterConfig = wlsDomainConfig.getClusterConfig(CLUSTER1);
    DomainConfig domainConfig = createDomainConfig(3);
    ClusterConfig clusterConfig = domainConfig.getClusters().get(CLUSTER1);
    ClusteredServerConfig clusteredServerConfig = clusterConfig.getServers().get(SERVER1);
    clusteredServerConfig.setStartedServerState(ClusteredServerConfig.STARTED_SERVER_STATE_ADMIN);

    Collection<DomainPresenceInfo.ServerStartupInfo> ssic =
        new ArrayList<DomainPresenceInfo.ServerStartupInfo>();
    Collection<String> servers = new ArrayList<String>();
    servers.add(SERVER0);

    int startedCount =
        ManagedServersUpStep.addClusteredServer(
            wlsDomainConfig,
            ssic,
            AS_NAME,
            servers,
            wlsClusterConfig,
            0,
            SERVER1,
            clusteredServerConfig);
    assertEquals(1, startedCount);
    assertEquals(1, ssic.size());

    DomainPresenceInfo.ServerStartupInfo ssi = ssic.iterator().next();
    List<V1EnvVar> envVars = ssi.envVars;
    assertEquals(1, envVars.size());
    V1EnvVar javaOptions = envVars.iterator().next();
    assertEquals("JAVA_OPTIONS", javaOptions.getName());
    assertNotNull(javaOptions.getValue());
    assertTrue(javaOptions.getValue().contains("-Dweblogic.management.startupMode=ADMIN"));
  }

  private DomainConfig createDomainConfig(int replicas) {
    return (new DomainConfig())
        .withCluster(
            CLUSTER1,
            (new ClusterConfig())
                .withClusterName(CLUSTER1)
                .withReplicas(replicas)
                .withServer(
                    SERVER0,
                    (new ClusteredServerConfig())
                        .withClusterName(CLUSTER1)
                        .withServerName(SERVER0)
                        .withClusteredServerStartPolicy(
                            ClusteredServerConfig.CLUSTERED_SERVER_START_POLICY_ALWAYS))
                .withServer(
                    SERVER1,
                    (new ClusteredServerConfig())
                        .withClusterName(CLUSTER1)
                        .withServerName(SERVER1)
                        .withClusteredServerStartPolicy(
                            ClusteredServerConfig.CLUSTERED_SERVER_START_POLICY_ALWAYS))
                .withServer(
                    SERVER2,
                    (new ClusteredServerConfig())
                        .withClusterName(CLUSTER1)
                        .withServerName(SERVER2)
                        .withClusteredServerStartPolicy(
                            ClusteredServerConfig.CLUSTERED_SERVER_START_POLICY_IF_NEEDED)))
        .withServer(
            SERVER3,
            (new NonClusteredServerConfig())
                .withServerName(SERVER3)
                .withNonClusteredServerStartPolicy(
                    NonClusteredServerConfig.NON_CLUSTERED_SERVER_START_POLICY_ALWAYS))
        .withServer(
            SERVER4,
            (new NonClusteredServerConfig())
                .withServerName(SERVER4)
                .withNonClusteredServerStartPolicy(
                    NonClusteredServerConfig.NON_CLUSTERED_SERVER_START_POLICY_ALWAYS));
  }

  private WlsDomainConfig createWLSDomainConfig(String json) {
    return WlsDomainConfig.create(json);
  }
}
