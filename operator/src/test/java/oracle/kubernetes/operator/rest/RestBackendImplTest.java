package oracle.kubernetes.operator.rest;

import io.kubernetes.client.models.V1UserInfo;
import java.util.ArrayList;
import java.util.Collection;
import oracle.kubernetes.operator.helpers.ClusterConfig;
import oracle.kubernetes.weblogic.domain.v1.Domain;

public class RestBackendImplTest {

  private static final String WEBLOGIC_DOMAIN = "weblogic-domain";
  private static final String CLUSTER1 = "cluster-1";

  // @Test
  public void isReplicaCountUpdated() {
    Collection<String> targetNamespaces = new ArrayList<>();
    targetNamespaces.add(WEBLOGIC_DOMAIN);
    RestBackendImpl restBackend = new MyRestBackendImpl("default", "accessToken", targetNamespaces);

    Domain domain = new Domain();
    boolean isReplicaCountUpdated =
        restBackend.isReplicaCountUpdated(WEBLOGIC_DOMAIN, domain, CLUSTER1, 2);
  }

  public static class MyRestBackendImpl extends RestBackendImpl {

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
  }
}
