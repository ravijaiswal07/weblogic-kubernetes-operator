// Copyright 2018, Oracle Corporation and/or its affiliates.  All rights reserved.
// Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.

package oracle.kubernetes.operator;

import java.util.ArrayList;
import java.util.HashMap;

import com.google.gson.Gson;
import static oracle.kubernetes.operator.utils.KubernetesArtifactUtils.*;
import static oracle.kubernetes.operator.utils.YamlUtils.*;
import oracle.kubernetes.weblogic.domain.v1.Cluster;
import oracle.kubernetes.weblogic.domain.v1.ClusterParams;
import oracle.kubernetes.weblogic.domain.v1.ClusterStartup;
import oracle.kubernetes.weblogic.domain.v1.ClusteredServer;
import oracle.kubernetes.weblogic.domain.v1.DomainSpec;
import oracle.kubernetes.weblogic.domain.v1.NonClusteredServer;
import oracle.kubernetes.weblogic.domain.v1.Server;
import oracle.kubernetes.weblogic.domain.v1.ServerStartup;
import oracle.kubernetes.weblogic.domain.v1.api.WeblogicApi;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import org.junit.Test;

public class CustomResourceTest {

  @Test
  public void convertDomainSpecToJsonAndBack_createsEqualObject() throws Exception {
    DomainSpec want = newPopulatedDomainSpec();
    Gson gson = (new WeblogicApi()).getApiClient().getJSON().getGson();
    String json = gson.toJson(want);
    DomainSpec have = gson.fromJson(json, DomainSpec.class);
    assertThat(have, equalTo(want));
  }

  @Test
  public void sameDomainSpecs_equalTrue() throws Exception {
    assertThat(
      newPopulatedDomainSpec(),
      equalTo(newPopulatedDomainSpec()));
  }

  @Test
  public void differentDomainSpecs_equalFalse() throws Exception {
    assertThat(
      newPopulatedDomainSpec().getClusters().get("cluster1").withReplicas(3),
      not(equalTo(newPopulatedDomainSpec())));
  }

  @Test
  public void convertDomainSpecToYamlAndBack_createsEqualObject() throws Exception {
    DomainSpec want = newPopulatedDomainSpec();
    String yaml = newYaml().dump(want);
    DomainSpec have = newYaml().loadAs(yaml, DomainSpec.class);
    assertThat(have, equalTo(want));
  }

  @Test
  public void newServer_hasCorrectDefaultValues() throws Exception {
    Server want = newServer();
    setExpectedServerDefaults(want);
    assertThat(newServer(), equalTo(want));
  }

  @Test
  public void newNonClusteredServer_hasCorrectDefaultValues() throws Exception {
    NonClusteredServer want = newNonClusteredServer().withNonClusteredServerStartPolicy(null);
    setExpectedServerDefaults(want);
    assertThat(newServer(), equalTo(want));
  }

  @Test
  public void newClusteredServer_hasCorrectDefaultValues() throws Exception {
    ClusteredServer want = newClusteredServer().withClusteredServerStartPolicy(null);
    setExpectedServerDefaults(want);
    assertThat(newServer(), equalTo(want));
  }

  @Test
  public void newCluster_hasCorrectDefaultValues() throws Exception {
    Cluster want = newCluster().withServerDefaults(null).withServers(new HashMap<String,ClusteredServer>());
    setExpectedClusterParamsDefaults(want);
    assertThat(newCluster(), equalTo(want));
  }

  @Test
  public void newClusterParams_hasCorrectDefaultValues() throws Exception {
    ClusterParams want = newClusterParams();
    setExpectedClusterParamsDefaults(want);
    assertThat(newClusterParams(), equalTo(want));
  }

  @Test
  public void newDomainSpec_hasCorrectDefaultValues() throws Exception {
    DomainSpec want = newDomainSpec()
      .withDomainUID(null)
      .withDomainName(null)
      .withImage(null)
      .withImagePullPolicy(null)
      .withAdminSecret(null)
      .withAsName(null)
      .withAsPort(null)
      .withExportT3Channels(new ArrayList<String>())
      .withStartupControl(null)
      .withServerStartup(new ArrayList<ServerStartup>())
      .withClusterStartup(new ArrayList<ClusterStartup>())
      .withServerDefaults(null)
      .withNonClusteredServerDefaults(null)
      .withServers(new HashMap<String,NonClusteredServer>())
      .withClusterDefaults(null)
      .withClusters(new HashMap<String,Cluster>())
      .withReplicas(null)
      .withUseNewLifeCycleConfig(null);
    assertThat(newDomainSpec(), equalTo(want));
  }

  private void setExpectedServerDefaults(Server server) {
    server
      .withStartedServerState(null)
      .withRestartedLabel(null)
      .withNodePort(null)
      .withEnv(null)
      .withImage(null)
      .withImagePullPolicy(null)
      .withImagePullSecrets(null)
      .withShutdownPolicy(null)
      .withGracefulShutdownTimeout(null)
      .withGracefulShutdownIgnoreSessions(null)
      .withGracefulShutdownWaitForSessions(null);
  }

  private void setExpectedClusterParamsDefaults(ClusterParams clusterParams) {
    clusterParams
      .withReplicas(null)
      .withMaxSurge(null)
      .withMaxUnavailable(null)
      .withServerDefaults(null);
  }

  private DomainSpec newPopulatedDomainSpec() {
    return
      newDomainSpec()
        .withNonClusteredServerDefaults(newNonClusteredServer()
          .withNonClusteredServerStartPolicy("ALWAYS"))
        .withServerDefaults(newServer()
          .withStartedServerState("ADMIN")
          .withRestartedLabel("label1")
          .withNodePort(30000)
          .withEnv(newEnvVarList()
            .addElement(newEnvVar().name("env1").value("val1"))
            .addElement(newEnvVar().name("env2").value("val2")))
          .withImage("image1")
          .withImagePullPolicy("Never")
          .withImagePullSecrets(newLocalObjectReferenceList()
            .addElement(newLocalObjectReference().name("secret1"))
            .addElement(newLocalObjectReference().name("secret2")))
          .withShutdownPolicy("GRACEFUL_SHUTDOWN")
          .withGracefulShutdownTimeout(120)
          .withGracefulShutdownIgnoreSessions(true)
          .withGracefulShutdownWaitForSessions(true))
        .withClusterDefaults(newClusterParams()
          .withReplicas(10)
          // Don't test IntOrString properties since equals doesn't work for them (this is a k8s bug):
          // .withMaxSurge(newIntOrString("10%"))
          // .withMaxUnavailable(newIntOrString(2))
          .withServerDefaults(newClusteredServer()
            .withClusteredServerStartPolicy("NEVER")))
        .withCluster("cluster1", newCluster()
          .withReplicas(10)
          .withServerDefaults(newClusteredServer()
            .withClusteredServerStartPolicy("ALWAYS"))
          .withServer("cluster1-server1", newClusteredServer()
            .withClusteredServerStartPolicy("IF_NEEDED"))
          .withServer("cluster1-server2", newClusteredServer()
            .withClusteredServerStartPolicy("NEVER")))
        .withCluster("cluster2", newCluster()
          .withServer("cluster2-server1", newClusteredServer()
            .withClusteredServerStartPolicy("IF_NEEDED")))
        .withServer("non-clustered-server1", newNonClusteredServer()
           .withNonClusteredServerStartPolicy("NEVER"))
        .withServer("non-clustered-server2", newNonClusteredServer()
           .withNonClusteredServerStartPolicy("ALWAYS"));
  }
}
