package oracle.kubernetes.operator.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import io.kubernetes.client.models.V1ObjectMeta;
import io.kubernetes.client.models.V1SecretReference;
import io.kubernetes.client.models.V1UserInfo;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.ws.rs.WebApplicationException;
import oracle.kubernetes.operator.LabelConstants;
import oracle.kubernetes.operator.StartupControlConstants;
import oracle.kubernetes.operator.VersionConstants;
import oracle.kubernetes.operator.helpers.AuthorizationProxy;
import oracle.kubernetes.operator.helpers.ClusterConfig;
import oracle.kubernetes.operator.rest.backend.RestBackend;
import oracle.kubernetes.operator.wlsconfig.WlsClusterConfig;
import oracle.kubernetes.operator.wlsconfig.WlsDomainConfig;
import oracle.kubernetes.weblogic.domain.v1.ClusterStartup;
import oracle.kubernetes.weblogic.domain.v1.Domain;
import oracle.kubernetes.weblogic.domain.v1.DomainSpec;
import org.junit.Test;

public class RestBackendImplTest {

  static final String JSON_STRING_1_CLUSTER =
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
  private static final String WEBLOGIC_DOMAIN = "weblogic-domain";
  private static final String CLUSTER1 = "cluster-1";
  private static final String AS_NAME = "admin-server";
  private static final String DOMAIN_UID = "domain1";
  private static final String ADMIN_SECRET = "adminSecret";

  private static Domain createV1Domain() {
    DomainSpec spec = new DomainSpec();
    spec.setStartupControl(StartupControlConstants.AUTO_STARTUPCONTROL);
    spec.setAsName(AS_NAME);
    spec.setDomainUID(DOMAIN_UID);
    V1SecretReference adminSecret =
        new V1SecretReference().namespace(WEBLOGIC_DOMAIN).name(ADMIN_SECRET);
    spec.setAdminSecret(adminSecret);
    ClusterStartup clusterStartup = new ClusterStartup().withClusterName(CLUSTER1).withReplicas(1);
    List<ClusterStartup> clusterStartups = new ArrayList<>();
    clusterStartups.add(clusterStartup);
    spec.setClusterStartup(clusterStartups);
    Map<String, String> labels = new HashMap<>();
    labels.put(LabelConstants.RESOURCE_VERSION_LABEL, VersionConstants.DOMAIN_V1);
    V1ObjectMeta v1ObjectMeta = new V1ObjectMeta();
    v1ObjectMeta.setLabels(labels);
    return new Domain().withSpec(spec).withMetadata(v1ObjectMeta);
  }

  private static WlsDomainConfig createWLSDomainConfig(String json) {
    return WlsDomainConfig.create(json);
  }

  @Test
  public void isReplicaCountUpdated() {
    RestBackendImpl restBackend = getRestBackend();

    Domain domain = createV1Domain();
    boolean isReplicaCountUpdated =
        restBackend.isReplicaCountUpdated(WEBLOGIC_DOMAIN, domain, CLUSTER1, 2);
    assertTrue(isReplicaCountUpdated);
  }

  private Collection<String> getTargetNamespaces() {
    Collection<String> targetNamespaces = new ArrayList<>();
    targetNamespaces.add(WEBLOGIC_DOMAIN);
    return targetNamespaces;
  }

  @Test
  public void scaleCluster_badRequestException() {
    try {
      RestBackendImpl restBackend = getRestBackend();
      restBackend.scaleCluster(DOMAIN_UID, CLUSTER1, -1);
      fail("Expected WebApplicationException for managedServerCount < 0");
    } catch (Exception wae) {
    }
  }

  @Test
  public void scaleCluster() {
    try {
      RestBackend restBackend = getRestBackend();
      restBackend.scaleCluster(DOMAIN_UID, CLUSTER1, 3);
    } catch (Exception wae) {
      wae.printStackTrace();
      fail("scaleCluster: Did not expect WebApplicationException wae: " + wae);
    }
  }

  @Test
  public void verifyWLSConfiguredClusterCapacity_badRequestException() {
    Domain domain = createV1Domain();
    try {
      RestBackendImpl restBackend = getRestBackend();
      restBackend.verifyWLSConfiguredClusterCapacity(DOMAIN_UID, domain, CLUSTER1, 4);
      fail("Expected WebApplicationException for managedServerCount > cluster size");
    } catch (WebApplicationException wae) {

    }
  }

  @Test
  public void verifyWLSConfiguredClusterCapacity() {
    Domain domain = createV1Domain();
    try {
      RestBackendImpl restBackend = getRestBackend();
      restBackend.verifyWLSConfiguredClusterCapacity(DOMAIN_UID, domain, CLUSTER1, 3);
    } catch (WebApplicationException wae) {
      fail("Didn't expect WebApplicationException wae: " + wae);
    }
  }

  private RestBackendImpl getRestBackend() {
    Collection<String> targetNamespaces = getTargetNamespaces();
    return new MyRestBackendImpl("default", "accessToken", targetNamespaces);
  }

  @Test
  public void isCluster() {
    RestBackendImpl restBackend = getRestBackend();
    restBackend.isCluster(DOMAIN_UID, CLUSTER1);
  }

  @Test
  public void getClusters() {
    RestBackendImpl restBackend = getRestBackend();
    Set<String> clusters = restBackend.getClusters(DOMAIN_UID);
    assertEquals(1, clusters.size());
    assertTrue(clusters.contains(CLUSTER1));
  }

  @Test
  public void getDomainUIDs() {
    RestBackendImpl restBackend = getRestBackend();
    Set<String> domainUIDs = restBackend.getDomainUIDs();
    assertEquals(1, domainUIDs.size());
    assertTrue(domainUIDs.contains(DOMAIN_UID));
  }

  @Test
  public void isDomainUID() {
    RestBackendImpl restBackend = getRestBackend();
    assertTrue(restBackend.isDomainUID(DOMAIN_UID));
  }

  private static class MyRestBackendImpl extends RestBackendImpl {

    /**
     * Construct a RestBackendImpl that is used to handle one WebLogic operator REST request.
     *
     * @param principal is the name of the Kubernetes user to use when calling the Kubernetes REST
     *     api.
     * @param accessToken is the access token of the Kubernetes service account of the client
     *     calling the WebLogic operator REST api.
     * @param targetNamespaces a list of Kubernetes namepaces that contain domains that the WebLogic
     */
    public MyRestBackendImpl(
        String principal, String accessToken, Collection<String> targetNamespaces) {
      super(principal, accessToken, targetNamespaces);
    }

    protected V1UserInfo authenticate(String accessToken) {
      return new V1UserInfo();
    }

    protected ClusterConfig getClusterConfig(Domain dom, String namespace, String cluster) {
      return new ClusterConfig().withClusterName(cluster).withReplicas(1);
    }

    protected WlsClusterConfig getWlsClusterConfig(
        String namespace, String cluster, String adminServerServiceName, String adminSecretName) {
      WlsDomainConfig wlsDomainConfig =
          getWlsDomainConfig(namespace, adminServerServiceName, adminSecretName);
      return wlsDomainConfig.getWlsClusterConfig(cluster);
    }

    protected WlsDomainConfig getWlsDomainConfig(
        String namespace, String adminServerServiceName, String adminSecretName) {
      return createWLSDomainConfig(JSON_STRING_1_CLUSTER);
    }

    protected void authorize(String domainUID, AuthorizationProxy.Operation operation) {}

    protected List<Domain> getDomainsList() {
      List<Domain> domains = new ArrayList<>();
      domains.add(createV1Domain());
      return domains;
    }

    protected void replaceDomain(String namespace, Domain domain, String domainUID) {}
  }
}
