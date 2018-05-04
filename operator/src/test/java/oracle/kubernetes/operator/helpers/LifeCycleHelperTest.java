// Copyright 2018, Oracle Corporation and/or its affiliates.  All rights reserved.
// Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.

package oracle.kubernetes.operator.helpers;

import java.beans.BeanInfo;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import com.meterware.simplestub.Memento;
import io.kubernetes.client.custom.IntOrString;
import oracle.kubernetes.TestUtils;
import static oracle.kubernetes.operator.KubernetesConstants.*;
import static oracle.kubernetes.operator.create.KubernetesArtifactUtils.*;
import static oracle.kubernetes.operator.create.YamlUtils.*;
import oracle.kubernetes.weblogic.domain.v1.DomainSpec;
import oracle.kubernetes.weblogic.domain.v1.Cluster;
import oracle.kubernetes.weblogic.domain.v1.ClusterParams;
import oracle.kubernetes.weblogic.domain.v1.ClusteredServer;
import oracle.kubernetes.weblogic.domain.v1.NonClusteredServer;
import oracle.kubernetes.weblogic.domain.v1.Server;
import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;
import org.junit.Test;

/**
 * Tests LifeCycleHelper
 */
public class LifeCycleHelperTest extends LifeCycleHelper {

// TBD - how do we hide the logged warnings so that the test output looks clean?

  private static final String PROPERTY_STARTED_SERVER_STATE = "startedServerState";
  private static final String PROPERTY_RESTARTED_LABEL = "restartedLabel";
  private static final String PROPERTY_NODE_PORT = "nodePort";
  private static final String PROPERTY_ENV = "env";
  private static final String PROPERTY_IMAGE = "image";
  private static final String PROPERTY_IMAGE_PULL_POLICY = "imagePullPolicy";
  private static final String PROPERTY_IMAGE_PULL_SECRETS = "imagePullSecrets";
  private static final String PROPERTY_SHUTDOWN_POLICY = "shutdownPolicy";
  private static final String PROPERTY_GRACEFUL_SHUTDOWN_TIMEOUT = "gracefulShutdownTimeout";
  private static final String PROPERTY_GRACEFUL_SHUTDOWN_IGNORE_SESSIONS = "gracefulShutdownIgnoreSessions";
  private static final String PROPERTY_GRACEFUL_SHUTDOWN_WAIT_FOR_SESSIONS = "gracefulShutdownWaitForSessions";
  private static final String PROPERTY_CLUSTERED_SERVER_START_POLICY = "clusteredServerStartPolicy";
  private static final String PROPERTY_NON_CLUSTERED_SERVER_START_POLICY = "nonClusteredServerStartPolicy";
  private static final String PROPERTY_REPLICAS = "replicas";
  private static final String PROPERTY_MAX_SURGE = "maxSurge";
  private static final String PROPERTY_MAX_UNAVAILABLE = "maxUnavailable";

  @Test 
  public void getEffectiveNonClusteredServerConfig_returnsCorrectConfig() {
    String serverName = "server1";
    NonClusteredServer server = newNonClusteredServer().withImage("image1");
    DomainSpec domainSpec = newDomainSpec().withServer(serverName, server);
    NonClusteredServerConfig ncsc = getEffectiveNonClusteredServerConfig(domainSpec, serverName);
    // Just spot check a few properties
    NonClusteredServerConfig actual = (new NonClusteredServerConfig())
      .withServerName(ncsc.getServerName())
      .withImage(ncsc.getImage())
      .withImagePullPolicy(ncsc.getImagePullPolicy())
      .withNonClusteredServerStartPolicy(ncsc.getNonClusteredServerStartPolicy());
    NonClusteredServerConfig want = (new NonClusteredServerConfig())
      .withServerName(serverName)
      .withImage(server.getImage())
      .withImagePullPolicy(IFNOTPRESENT_IMAGEPULLPOLICY)
      .withNonClusteredServerStartPolicy(NON_CLUSTERED_SERVER_START_POLICY_ALWAYS);
    assertThat(actual, equalTo(want));
  }

  @Test 
  public void getEffectiveClusteredServerConfig_returnsCorrectConfig() {
    String clusterName = "cluster1";
    String serverName = "server1";
    ClusteredServer server = newClusteredServer().withImage("image1");
    Cluster cluster = newCluster().withServer(serverName, server);
    DomainSpec domainSpec = newDomainSpec().withCluster(clusterName, cluster);
    ClusteredServerConfig csc = getEffectiveClusteredServerConfig(domainSpec, clusterName, serverName);
    // Just spot check a few properties
    ClusteredServerConfig actual = (new ClusteredServerConfig())
      .withServerName(csc.getServerName())
      .withClusterName(csc.getClusterName())
      .withImage(csc.getImage())
      .withImagePullPolicy(csc.getImagePullPolicy())
      .withClusteredServerStartPolicy(csc.getClusteredServerStartPolicy());
    ClusteredServerConfig want = (new ClusteredServerConfig())
      .withServerName(serverName)
      .withClusterName(clusterName)
      .withImage(server.getImage())
      .withImagePullPolicy(IFNOTPRESENT_IMAGEPULLPOLICY)
      .withClusteredServerStartPolicy(CLUSTERED_SERVER_START_POLICY_IF_NEEDED);
    assertThat(actual, equalTo(want));
  }

  @Test 
  public void getEffectiveClusterConfig_returnsCorrectConfig() {
    String clusterName = "cluster1";
    Cluster cluster = newCluster().withReplicas(100).withMaxSurge(newIntOrString("30%"));
    DomainSpec domainSpec = newDomainSpec().withCluster(clusterName, cluster);
    ClusterConfig cc = getEffectiveClusterConfig(domainSpec, clusterName);
    // Just spot check a few properties
    ClusterConfig actual = (new ClusterConfig())
      .withClusterName(cc.getClusterName())
      .withReplicas(cc.getReplicas())
      .withMinReplicas(cc.getMinReplicas())
      .withMaxReplicas(cc.getMaxReplicas());
    ClusterConfig want = (new ClusterConfig())
      .withClusterName(clusterName)
      .withReplicas(cluster.getReplicas())
      .withMinReplicas(80) // 100 - 20%(100) since maxUnavailable defaults to 20%
      .withMaxReplicas(130); // 100 + 30%(100)
    assertThat(actual, equalTo(want));
  }

  @Test
  public void toNonClusteredServerConfig_createsCorrectConfig() {
    String serverName = "server1";
    NonClusteredServer ncs = newNonClusteredServer()
      .withNonClusteredServerStartPolicy(NON_CLUSTERED_SERVER_START_POLICY_NEVER)
      .withRestartedLabel("label1");
    NonClusteredServerConfig ncsc = toNonClusteredServerConfig(serverName, ncs);
    // toNonClusteredServer calls copyServerProperties, which fills in the default values
    // we've already tested this separately.
    // just test that toNonClusteredServer filled in the server name,
    // one property from the server level (restartedLabel) and
    // nonClusteredServerStartPolicy (which copyServerProperties doesn't handle)
    NonClusteredServerConfig actual = (new NonClusteredServerConfig())
      .withServerName(ncsc.getServerName())
      .withRestartedLabel(ncsc.getRestartedLabel())
      .withNonClusteredServerStartPolicy(ncsc.getNonClusteredServerStartPolicy());
    NonClusteredServerConfig want = (new NonClusteredServerConfig())
      .withServerName(serverName)
      .withRestartedLabel(ncs.getRestartedLabel())
      .withNonClusteredServerStartPolicy(ncs.getNonClusteredServerStartPolicy());
    assertThat(actual, equalTo(want));
  }

  @Test
  public void toClusteredServerConfig_createsCorrectConfig() {
    String clusterName = "cluster1";
    String serverName = "server1";
    ClusteredServer cs = newClusteredServer()
      .withClusteredServerStartPolicy(CLUSTERED_SERVER_START_POLICY_NEVER)
      .withRestartedLabel("label1");
    ClusteredServerConfig csc = toClusteredServerConfig(clusterName, serverName, cs);
    // toClusteredServer calls copyServerProperties, which fills in the default values
    // we've already tested this separately.
    // just test that toClusteredServer filled in the cluster name, server name,
    // one property from the server level (restartedLabel) and
    // clusteredServerStartPolicy (which copyServerProperties doesn't handle)
    ClusteredServerConfig actual = (new ClusteredServerConfig())
      .withClusterName(csc.getClusterName())
      .withServerName(csc.getServerName())
      .withRestartedLabel(csc.getRestartedLabel())
      .withClusteredServerStartPolicy(csc.getClusteredServerStartPolicy());
    ClusteredServerConfig want = (new ClusteredServerConfig())
      .withClusterName(clusterName)
      .withServerName(serverName)
      .withRestartedLabel(cs.getRestartedLabel())
      .withClusteredServerStartPolicy(cs.getClusteredServerStartPolicy());
    assertThat(actual, equalTo(want));
  }

  @Test
  public void toClusterConfig_createsCorrectConfig() {
    String clusterName = "cluster1";
    Cluster cluster = newCluster()
      .withReplicas(10)
      .withMaxSurge(newIntOrString("11%"))
      .withMaxUnavailable(newIntOrString(3));
    ClusterConfig actual = toClusterConfig(clusterName, cluster);
    ClusterConfig want = (new ClusterConfig())
      .withClusterName(clusterName)
      .withReplicas(cluster.getReplicas())
      .withMaxReplicas(12)
      .withMinReplicas(7);
    assertThat(actual, equalTo(want));
  }

  @Test
  public void copyServerProperties_allPropertiesSet_copiesProperties() {
    String serverName = "server1";
    Server s = (new Server())
      .withStartedServerState(STARTED_SERVER_STATE_ADMIN)
      .withRestartedLabel("label1")
      .withNodePort(30003)
      .withEnv(newEnvVarList().addElement(newEnvVar().name("env1").value("val1")))
      .withImage("image1")
      .withImagePullPolicy(NEVER_IMAGEPULLPOLICY)
      .withImagePullSecrets(newLocalObjectReferenceList().addElement(newLocalObjectReference().name("secret1")))
      .withShutdownPolicy(SHUTDOWN_POLICY_GRACEFUL_SHUTDOWN)
      .withGracefulShutdownTimeout(120)
      .withGracefulShutdownIgnoreSessions(true)
      .withGracefulShutdownWaitForSessions(true);
    ServerConfig actual = new ServerConfig();
    copyServerPropertiesToServerConfig(serverName, s, actual);
    ServerConfig want = (new ServerConfig())
      .withServerName(serverName)
      .withStartedServerState(s.getStartedServerState())
      .withRestartedLabel(s.getRestartedLabel())
      .withNodePort(s.getNodePort())
      .withEnv(s.getEnv())
      .withImage(s.getImage())
      .withImagePullPolicy(s.getImagePullPolicy())
      .withImagePullSecrets(s.getImagePullSecrets())
      .withShutdownPolicy(s.getShutdownPolicy())
      .withGracefulShutdownTimeout(s.getGracefulShutdownTimeout())
      .withGracefulShutdownIgnoreSessions(s.getGracefulShutdownIgnoreSessions())
      .withGracefulShutdownWaitForSessions(s.getGracefulShutdownWaitForSessions());
    assertThat(actual, equalTo(want));
  }

  @Test
  public void copyServerProperties_noPropertiesSet_fillsInDefaultValues() {
    ServerConfig actual = new ServerConfig();
    copyServerPropertiesToServerConfig(null, newServer(), actual);
    ServerConfig want = (new ServerConfig())
      .withStartedServerState(STARTED_SERVER_STATE_RUNNING)
      .withNodePort(0)
      .withImage(DEFAULT_IMAGE)
      .withImagePullPolicy(IFNOTPRESENT_IMAGEPULLPOLICY)
      .withShutdownPolicy(SHUTDOWN_POLICY_FORCED_SHUTDOWN)
      .withGracefulShutdownTimeout(0)
      .withGracefulShutdownIgnoreSessions(false)
      .withGracefulShutdownWaitForSessions(false);
    assertThat(actual, equalTo(want));
  }

  @Test
  public void getDefaultImagePullPolicy_nullImage_returns_ifNotPresent() {
    assertThat(getDefaultImagePullPolicy(null), equalTo(IFNOTPRESENT_IMAGEPULLPOLICY));
  }

  @Test
  public void getDefaultImagePullPolicy_imageDoesNotEndWithLatest_returns_ifNotPresent() {
    assertThat(getDefaultImagePullPolicy("image1"), equalTo(IFNOTPRESENT_IMAGEPULLPOLICY));
  }

  @Test
  public void getImagePullPolicy_imageEndsWithLatest_returns_always() {
    assertThat(getDefaultImagePullPolicy("image1" + LATEST_IMAGE_SUFFIX), equalTo(ALWAYS_IMAGEPULLPOLICY));
  }

  @Test
  public void getMaxReplicas_intMaxSurge_returns_replicasPlusMaxSurge() {
    int replicas = 10;
    int maxUnavailable = 2;
    assertThat(getMaxReplicas(replicas, new IntOrString(maxUnavailable)), equalTo(replicas + maxUnavailable));
  }

  @Test
  public void getMaxReplicas_percentMaxSurge_returns_replicasPlusPercentageOfReplica() {
    assertThat(getMaxReplicas(10, new IntOrString("21%")), equalTo(13));
  }

  @Test
  public void getMinReplicas_zeroReplicas_intMaxUnavailable_returns_0() {
    assertThat(getMinReplicas(0, new IntOrString(2)), equalTo(0));
  }

  @Test
  public void getMinReplicas_zeroReplicas_percentMaxUnavailable_returns_0() {
    assertThat(getMinReplicas(0, new IntOrString("100%")), equalTo(0));
  }

  @Test
  public void getMinReplicas_intMaxUnavailableLessThanReplicas_returns_replicasMinusMaxUnavailable() {
    int replicas = 10;
    int maxUnavailable = 2;
    assertThat(getMinReplicas(replicas, new IntOrString(maxUnavailable)), equalTo(replicas - maxUnavailable));
  }

  @Test
  public void getMinReplicas_intMaxUnavailableGreaterThanReplicas_returns_0() {
    int replicas = 10;
    assertThat(getMinReplicas(10, new IntOrString(11)), equalTo(0));
  }

  @Test
  public void getMinReplicas_percentMaxUnavailable_returns_replicasMinusPercentageOfReplicas() {
    assertThat(getMinReplicas(10, new IntOrString("22%")), equalTo(7));
  }

  @Test 
  public void getEffectiveClusteredServer_haveServer_haveClusterHasServerDefaults_haveClusterDefaultsHasServerDefaults_haveClusteredServerDefaults_haveServerDefaults_returnsCorrectParents() {

    String server1Name = "server1";
    ClusteredServer server1 = newClusteredServer();
    ClusteredServer cluster1ServerDefaults = newClusteredServer();
    ClusteredServer clusterDefaultsServerDefaults = newClusteredServer();
    Server serverDefaults = newServer();
    ClusteredServer want = withClusteredServerDefaults(newClusteredServer());

    String cluster1Name = "cluster1";
    Cluster cluster1 = newCluster()
      .withServerDefaults(cluster1ServerDefaults)
      .withServer(server1Name, server1)
      .withServer("server2", newClusteredServer());
    ClusterParams clusterDefaults = (newClusterParams())
      .withServerDefaults(clusterDefaultsServerDefaults);
    DomainSpec domainSpec = newDomainSpec()
      .withCluster(cluster1Name, cluster1)
      .withClusterDefaults(clusterDefaults)
      .withServerDefaults(serverDefaults);

    server1.setImage("image1");
    cluster1ServerDefaults.setImage("image2"); // ignored because set on server1
    clusterDefaultsServerDefaults.setImage("image3"); // ignored because set on server1
    serverDefaults.setImage("image4"); // ignored because set on server1
    want.setImage(server1.getImage());

    cluster1ServerDefaults.setGracefulShutdownTimeout(new Integer(200)); // used because not set on server1
    clusterDefaultsServerDefaults.setGracefulShutdownTimeout(new Integer(300)); // ignored because set on cluster1ServerDefaults
    serverDefaults.setGracefulShutdownTimeout(new Integer(400)); // ignored because set on cluster1ServerDefaults
    want.setGracefulShutdownTimeout(cluster1ServerDefaults.getGracefulShutdownTimeout());

    clusterDefaultsServerDefaults.setRestartedLabel("label1"); // used because not set on server1 or cluster1ServerDefaults
    serverDefaults.setRestartedLabel("label2"); // ignored because set on clusterDefaultsServerDefaults
    want.setRestartedLabel(clusterDefaultsServerDefaults.getRestartedLabel());

    serverDefaults.setStartedServerState(STARTED_SERVER_STATE_ADMIN); // used because not set on server1, cluster1ServerDefaults or clusterDefaultsServerDefaults
    want.setStartedServerState(serverDefaults.getStartedServerState());

    ClusteredServer actual = getEffectiveClusteredServer(domainSpec, cluster1Name, server1Name);
    assertThat(actual, equalTo(want));
  }

  @Test 
  public void getEffectivClusteredServer_noServer_noCluster_noClusterDefaults_noServerDefaults_returnsBakedInDefaults() {
    ClusteredServer actual = getEffectiveClusteredServer(newDomainSpec(), "cluster1", "server1");
    ClusteredServer want = withClusteredServerDefaults(newClusteredServer());
    assertThat(actual, equalTo(want));
  }

  @Test 
  public void getEffectiveNonClusteredServer_haveServer_haveNonClusteredServerDefaults_haveServerDefaults_returnsNearestProperties() {

    String server1Name = "server1";
    NonClusteredServer server1 = newNonClusteredServer();
    NonClusteredServer nonClusteredServerDefaults = newNonClusteredServer();
    Server serverDefaults = newServer();
    NonClusteredServer want = withNonClusteredServerDefaults(newNonClusteredServer());

    DomainSpec domainSpec = newDomainSpec()
      .withServer(server1Name, server1)
      .withNonClusteredServerDefaults(nonClusteredServerDefaults)
      .withServerDefaults(serverDefaults);

    server1.withNodePort(new Integer(20)); // used because set on server1
    nonClusteredServerDefaults.withNodePort(new Integer(25)); // ignored because set on server1
    serverDefaults.withNodePort(new Integer(30)); // ignored because set on server1
    want.setNodePort(server1.getNodePort());

    nonClusteredServerDefaults
      .withImagePullSecrets(newLocalObjectReferenceList() // used because not set on server1
        .addElement(newLocalObjectReference().name("secret1"))
        .addElement(newLocalObjectReference().name("secret2")));
    serverDefaults
      .withImagePullSecrets(newLocalObjectReferenceList() // ignored because set on nonClusteredServerDefaults
        .addElement(newLocalObjectReference().name("secret3"))
        .addElement(newLocalObjectReference().name("secret4"))
        .addElement(newLocalObjectReference().name("secret5")));
    want.setImagePullSecrets(nonClusteredServerDefaults.getImagePullSecrets());

    serverDefaults
      .withEnv(newEnvVarList() // used because not set on server1 or nonClusteredServerDefaults
        .addElement(newEnvVar().name("env1").value("val1"))
        .addElement(newEnvVar().name("env2").value("val2")));
    want.setEnv(serverDefaults.getEnv());

    NonClusteredServer actual = getEffectiveNonClusteredServer(domainSpec, server1Name);
    assertThat(actual, yamlEqualTo(want));
  }

  @Test 
  public void getEffectiveNonClusteredServer_noServer_noNonClusteredServerDefaults_noServerDefaults_returnsBakedInDefaults() {
    NonClusteredServer actual = getEffectiveNonClusteredServer(newDomainSpec(), "server1");
    NonClusteredServer want = withNonClusteredServerDefaults(newNonClusteredServer());
  }

  @Test
  public void getEffectiveCluster_haveCluster_haveClusterDefaults_returnsNearestProperties() {
    String cluster1Name = "cluster1";
    Cluster cluster1 = newCluster();
    ClusterParams clusterDefaults = newClusterParams();
    Cluster want = withClusterDefaults(newCluster());

    DomainSpec domainSpec = newDomainSpec()
      .withCluster(cluster1Name, cluster1)
      .withClusterDefaults(clusterDefaults);

    cluster1.withMaxSurge(newIntOrString("30%")); // used because set on server1
    clusterDefaults.withMaxSurge(newIntOrString(40)); // ignored because set on server1
    want.setMaxSurge(cluster1.getMaxSurge());

    clusterDefaults.withReplicas(new Integer(6)); // used because not set on server1
    want.setReplicas(clusterDefaults.getReplicas());

    Cluster actual = getEffectiveCluster(domainSpec, cluster1Name);
    // IntOrString.equals is broken, so convert to sorted yaml and compare that
    assertThat(actual, yamlEqualTo(want));
  }

  @Test 
  public void getEffectiveCluster_noCluster_noClusterDefaults_returnsBakedInDefaults() {
    Cluster actual = getEffectiveCluster(newDomainSpec(), "cluster1");
    Cluster want = withClusterDefaults(newCluster());
    // IntOrString.equals is broken, so convert to sorted yaml and compare that
    assertThat(actual, yamlEqualTo(want));
  }

  @Test
  public void getClusteredServerParents_haveServer_haveClusterHasServerDefaults_haveClusterDefaultsHasServerDefaults_haveClusteredServerDefaults_haveServerDefaults_returnsCorrectParents() {

    String server1Name = "server1";
    ClusteredServer server1 = newClusteredServer();
    ClusteredServer cluster1ServerDefaults = newClusteredServer();
    ClusteredServer clusterDefaultsServerDefaults = newClusteredServer();
    Server serverDefaults = newServer();

    String cluster1Name = "cluster1";
    Cluster cluster1 = newCluster()
      .withServerDefaults(cluster1ServerDefaults)
      .withServer(server1Name, server1)
      .withServer("server2", newClusteredServer());
    ClusterParams clusterDefaults = (newClusterParams())
      .withServerDefaults(clusterDefaultsServerDefaults);
    DomainSpec domainSpec = newDomainSpec()
      .withCluster(cluster1Name, cluster1)
      .withClusterDefaults(clusterDefaults)
      .withServerDefaults(serverDefaults);

    // Disambiguate the parents so that equalTo fails when comparing different parents:
    server1.withGracefulShutdownTimeout(new Integer(50));
    cluster1ServerDefaults.withGracefulShutdownTimeout(new Integer(51));
    serverDefaults.withGracefulShutdownTimeout(new Integer(52));

    List<Object> actual = getClusteredServerParents(domainSpec, cluster1Name, server1Name);
    List<Object> want= toList(
      server1,
      cluster1ServerDefaults,
      clusterDefaultsServerDefaults,
      serverDefaults,
      CLUSTERED_SERVER_DEFAULTS,
      SERVER_DEFAULTS);
    assertThat(actual, equalTo(want));
  }

  @Test
  public void getClusteredServerParents_noServer_noCluster_noClusterDefaults_noClusteredServerDefaults_noServerDefaults_returnsCorrectParents() {
    List<Object> actual = getClusteredServerParents(newDomainSpec(), "cluster1", "server1");
    List<Object> want= toList(
      null, // no server
      CLUSTERED_SERVER_DEFAULTS,
      SERVER_DEFAULTS);
    assertThat(actual, equalTo(want));
  }

  @Test
  public void getNonClusteredServerParents_haveServer_haveNonClusteredServerDefaults_haveServerDefaults_returnsCorrectParents() {
    String server1Name = "server1";
    NonClusteredServer server1 = newNonClusteredServer();
    NonClusteredServer nonClusteredServerDefaults = newNonClusteredServer();
    Server serverDefaults = newServer();

    DomainSpec domainSpec = newDomainSpec()
      .withServerDefaults(serverDefaults)
      .withNonClusteredServerDefaults(nonClusteredServerDefaults)
      .withServer(server1Name, server1)
      .withServer("server2", newNonClusteredServer());

    // Disambiguate the parents so that equalTo fails when comparing different parents:
    server1.withRestartedLabel("label1");
    nonClusteredServerDefaults.withRestartedLabel("label2");
    serverDefaults.withRestartedLabel("label3");

    List<Object> actual = getNonClusteredServerParents(domainSpec, server1Name);
    List<Object> want= toList(
      server1,
      nonClusteredServerDefaults,
      serverDefaults,
      NON_CLUSTERED_SERVER_DEFAULTS,
      SERVER_DEFAULTS);
    assertThat(actual, equalTo(want));
  }

  @Test
  public void getNonClusteredServerParents_noServer_noNonClusteredServerDefaults_noServerDefaults_returnsCorrectParents() {
    List<Object> actual = getNonClusteredServerParents(newDomainSpec(), "server1");
    List<Object> want= toList(
      null, // no server
      null, // no domain non-clustered server defaults
      null, // no domain server defaults
      NON_CLUSTERED_SERVER_DEFAULTS,
      SERVER_DEFAULTS);
    assertThat(actual, equalTo(want));
  }

  @Test
  public void getClusterParents_haveCluster_haveClusterDefaults_returnsCorrectParents() {
    String cluster1Name = "cluster1";
    Cluster cluster1 = newCluster();
    ClusterParams clusterDefaults = newClusterParams();

    DomainSpec domainSpec = newDomainSpec()
      .withClusterDefaults(clusterDefaults)
      .withCluster(cluster1Name, cluster1)
      .withCluster("cluster2", newCluster());

    // Disambiguate the parents so that equalTo fails when comparing different parents:
    cluster1.withReplicas(new Integer(5));
    clusterDefaults.withReplicas(new Integer(7));

    List<Object> actual = getClusterParents(domainSpec, cluster1Name);
    List<Object> want= toList(cluster1, clusterDefaults, CLUSTER_DEFAULTS);
    assertThat(actual, equalTo(want));
  }

  @Test
  public void getClusterParents_noCluster_noClusterDefaults_returnsCorrectParents() {
    List<Object> actual = getClusterParents(newDomainSpec(), "cluster1");
    List<Object> want= toList(
      null, // no cluster
      null, // no domain cluster defaults
      CLUSTER_DEFAULTS);
    assertThat(actual, equalTo(want));
  }

  @Test
  public void getEffectiveProperties_copiesNearestParentProperties() {

    Server greatGrandParent = newServer();
    Server grandParent = null; // test that null parents are allowed
    Server parent = newServer();
    Server actual = newServer();
    Server want = newServer();

    // set on great grand parent, set on parent, set on actual: use actual
    greatGrandParent.setGracefulShutdownTimeout(new Integer(60));
    parent.setGracefulShutdownTimeout(new Integer(70));
    actual.setGracefulShutdownTimeout(new Integer(50));
    want.setGracefulShutdownTimeout(actual.getGracefulShutdownTimeout());

    // set on great grand parent, not set on parent, set on actual: use actual
    greatGrandParent.setShutdownPolicy(SHUTDOWN_POLICY_GRACEFUL_SHUTDOWN);
    actual.setShutdownPolicy(SHUTDOWN_POLICY_FORCED_SHUTDOWN);
    want.setShutdownPolicy(actual.getShutdownPolicy());

    // set on great grand parent, not set on parent, not set on actual : use great grand parent
    greatGrandParent.setGracefulShutdownIgnoreSessions(Boolean.TRUE);
    want.setGracefulShutdownIgnoreSessions(greatGrandParent.getGracefulShutdownIgnoreSessions());

    // not set on great grand parent, set on parent, set on actual : use actual
    parent.setStartedServerState(STARTED_SERVER_STATE_ADMIN);
    actual.setStartedServerState(STARTED_SERVER_STATE_RUNNING);
    want.setStartedServerState(actual.getStartedServerState());

    // not set on great grand parent, set on parent, not set on actual : use parent
    parent.setRestartedLabel("label123");
    want.setRestartedLabel(parent.getRestartedLabel());

    // not set on great grand parent, not set on parent, set on actual : use actual
    actual.setImage("image1");
    want.setImage(actual.getImage());

    List<Object> parents = new ArrayList();
    parents.add(parent);
    parents.add(grandParent);
    parents.add(greatGrandParent);

    getEffectiveProperties(
      actual,
      parents,
      PROPERTY_GRACEFUL_SHUTDOWN_TIMEOUT,
      PROPERTY_SHUTDOWN_POLICY,
      PROPERTY_GRACEFUL_SHUTDOWN_IGNORE_SESSIONS,
      PROPERTY_STARTED_SERVER_STATE,
      PROPERTY_RESTARTED_LABEL,
      PROPERTY_IMAGE);

    assertThat(actual, equalTo(want));
  }

  @Test
  public void copyParentProperties_copiesUnsetProperties() {

    Server actual = newServer();
    Server parent = newServer();
    Server want = newServer();

    // set on parent, set on actual : use actual
    parent.setStartedServerState(STARTED_SERVER_STATE_ADMIN);
    actual.setStartedServerState(STARTED_SERVER_STATE_RUNNING);
    want.setStartedServerState(actual.getStartedServerState());

    // set on parent, not set on actual : use parent
    parent.setRestartedLabel("newlabel");
    want.setRestartedLabel(parent.getRestartedLabel());

    // not set on parent, set on actual: use actual
    actual.setNodePort(new Integer(30000));
    want.setNodePort(actual.getNodePort());

    copyParentProperties(
      actual,
      getServerBI(),
      parent,
      getServerBI(),
      PROPERTY_STARTED_SERVER_STATE,
      PROPERTY_RESTARTED_LABEL,
      PROPERTY_NODE_PORT);

    assertThat(actual, equalTo(want));
  }

  @Test
  public void setPropertyIfUnsetAndHaveValue_propertyNotSet_dontHaveValue_doesNotSetPropertyValue() {
    String propertyName = PROPERTY_SHUTDOWN_POLICY;
    Server to = newServer();
    setPropertyIfUnsetAndHaveValue(to, getServerBI(), propertyName, null);
    assertThat(to, hasProperty(propertyName, nullValue()));
  }

  @Test
  public void setPropertyIfUnsetAndHaveValue_propertySet_dontHaveValue_doesNotSetPropertyValue() {
    String propertyName = PROPERTY_SHUTDOWN_POLICY;
    String oldPropertyValue = SHUTDOWN_POLICY_GRACEFUL_SHUTDOWN;
    Server to = newServer();
    to.setShutdownPolicy(oldPropertyValue);
    setPropertyIfUnsetAndHaveValue(to, getServerBI(), propertyName, null);
    assertThat(to, hasProperty(propertyName, equalTo(oldPropertyValue)));
  }

  @Test
  public void setPropertyIfUnsetAndHaveValue_propertySet_haveValue_doesNotSetPropertyValue() {
    String propertyName = PROPERTY_IMAGE_PULL_POLICY;
    String oldPropertyValue = ALWAYS_IMAGEPULLPOLICY;
    String newPropertyValue = IFNOTPRESENT_IMAGEPULLPOLICY;
    Server to = newServer();
    to.setImagePullPolicy(oldPropertyValue);
    setPropertyIfUnsetAndHaveValue(to, getServerBI(), propertyName, newPropertyValue);
    assertThat(to, hasProperty(propertyName, equalTo(oldPropertyValue)));
  }

  @Test
  public void setPropertyIfUnsetAndHaveValue_propertyNotSet_haveValue_setsPropertyValue() {
    String propertyName = PROPERTY_IMAGE;
    String newPropertyValue = "image2";
    Server to = newServer();
    setPropertyIfUnsetAndHaveValue(to, getServerBI(), propertyName, newPropertyValue);
    assertThat(to, hasProperty(propertyName, equalTo(newPropertyValue)));
  }

  @Test
  public void getProperty_propertyDoesNotExist_returnsNull() {
    assertThat(getProperty(newServer(), null), nullValue());
  }

  @Test
  public void getProperty_propertyExists_propertyNotSet_returnsNull() {
    assertThat(
      getProperty(newServer(), getPropertyDescriptor(getServerBI(), PROPERTY_NODE_PORT)),
      nullValue());
  }

  @Test
  public void getProperty_propertyExists_propertySet_returnsPropertyValue() {
    String propertyValue = "label123";
    Server server = newServer();
    server.setRestartedLabel(propertyValue);
    assertThat(
      getProperty(server, getPropertyDescriptor(getServerBI(), PROPERTY_RESTARTED_LABEL)),
      equalTo(propertyValue));
  }

  @Test
  public void getPropertyDescriptor_existingProperty_returnsPropertyDescriptor() {
    String propertyName = PROPERTY_STARTED_SERVER_STATE;
    PropertyDescriptor pd = getPropertyDescriptor(getServerBI(), propertyName);
    assertThat(pd, notNullValue());
    assertThat(pd.getName(), equalTo(propertyName));
  }

  @Test
  public void getPropertyDescriptor_nonExistingProperty_returnsPropertyDescriptor() {
    assertThat(getPropertyDescriptor(getServerBI(), "noSuchProperty"), nullValue());
  }

  @Test
  public void getBeanInfo_returnsBeanInfo() {
    BeanInfo bi = getServerBI();
    assertThat(bi, notNullValue());
    assertThat(bi.getBeanDescriptor().getBeanClass(), equalTo(Server.class));
  }

  @Test
  public void test_getPercent_nonFraction_doesntRoundUp() {
    assertThat(getPercentage(10, 50), equalTo(5));
  }

  @Test
  public void test_getPercent_fraction_roundsUp() {
    assertThat(getPercentage(10, 11), equalTo(2));
  }

  @Test
  public void test_getPercent_zero_returns_zero() {
    assertThat(getPercentage(0, 100), equalTo(0));
  }

  @Test
  public void test_getPercent_null_returns_0() {
    assertThat(getPercent("reason", null), equalTo(0));
  }

  @Test
  public void test_getPercent_noPercentSign_returns_0() {
    assertThat(getPercent("reason", "123"), equalTo(0));
  }

  @Test
  public void test_getPercent_nonInteger_returns_0() {
    assertThat(getPercent("reason", "12.3%"), equalTo(0));
  }

  @Test
  public void test_getPercent_negative_returns_0() {
    assertThat(getPercent("reason", "-1%"), equalTo(0));
  }

  @Test
  public void test_getPercent_zero_returns_0() {
    int percent = 0;
    assertThat(getPercent("reason", percent + "%"), equalTo(percent));
  }

  @Test
  public void test_getPercent_100_returns_0() {
    int percent = 100;
    assertThat(getPercent("reason", percent + "%"), equalTo(percent));
  }

  @Test
  public void test_getPercent_over100_returns_0() {
    assertThat(getPercent("reason", "101%"), equalTo(0));
  }

  @Test
  public void test_getPercent_between0And100_returns_percent() {
    int percent = 54;
    assertThat(getPercent("reason", percent + "%"), equalTo(percent));
  }

  @Test
  public void test_getNonNegativeInt_positveValue_returns_value() {
    int val = 5;
    assertThat(getNonNegativeInt("reason", val), equalTo(val));
  }

  @Test
  public void test_getNonNegativeInt_zero_returns_0() {
    assertThat(getNonNegativeInt("reason", 0), equalTo(0));
  }

  @Test
  public void test_getNonNegativeInt_negativeValue_returns_0() {
    assertThat(getNonNegativeInt("reason", -1), equalTo(0));
  }

  @Test
  public void toInt_int_returns_int() {
    int val = 7;
    assertThat(toInt(new Integer(val)), equalTo(val));
  }

  @Test
  public void toInt_null_returns_0() {
    assertThat(toInt(null), equalTo(0));
  }

  @Test
  public void toBool_val_returns_val() {
    boolean val = true;
    assertThat(toBool(new Boolean(val)), equalTo(val));
  }

  @Test
  public void toBool_null_returns_false() {
    assertThat(toBool(null), equalTo(false));
  }

  @Override
  protected void logWarning(String context, String message) {
    Memento memento = TestUtils.silenceOperatorLogger();
    try {
      super.logWarning(context, message);
    } finally {
      memento.revert();
    }
  }

  private NonClusteredServer withNonClusteredServerDefaults(NonClusteredServer nonClusteredServer) {
    withServerDefaults(nonClusteredServer);
    return nonClusteredServer.withNonClusteredServerStartPolicy(NON_CLUSTERED_SERVER_START_POLICY_ALWAYS);
  }

  private ClusteredServer withClusteredServerDefaults(ClusteredServer clusteredServer) {
    withServerDefaults(clusteredServer);
    return clusteredServer.withClusteredServerStartPolicy(CLUSTERED_SERVER_START_POLICY_IF_NEEDED);
  }

  private Server withServerDefaults(Server server) {
    return server
      .withStartedServerState(STARTED_SERVER_STATE_RUNNING)
      // no restartedLabel value
      // no nodePort value
      // no env value
      .withImage(DEFAULT_IMAGE)
      // no default image pull policy since it gets computed based on the image
      // no imagePullSecrets value
      .withShutdownPolicy(SHUTDOWN_POLICY_FORCED_SHUTDOWN)
      .withGracefulShutdownTimeout(new Integer(0))
      .withGracefulShutdownIgnoreSessions(Boolean.FALSE)
      .withGracefulShutdownWaitForSessions(Boolean.FALSE);
  }

  private Cluster withClusterDefaults(Cluster cluster) {
    withClusterParamsDefaults(cluster);
    return cluster;
  }

  private ClusterParams withClusterParamsDefaults(ClusterParams clusterParams) {
    return clusterParams
      // no default replicas value
      .withMaxSurge(newIntOrString("20%"))
      .withMaxUnavailable(newIntOrString("20%"));
  }

  private BeanInfo getServerBI() {
    return getBeanInfo(newServer());
  }

  private <T> List<T> toList(T... vals) {
    List<T> list = new ArrayList();
    for (T val : vals) {
      list.add(val);
    }
    return list;
  }
}
