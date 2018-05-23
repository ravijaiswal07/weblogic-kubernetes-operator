// Copyright 2018, Oracle Corporation and/or its affiliates.  All rights reserved.
// Licensed under the Universal Permissive License v 1.0 as shown at
// http://oss.oracle.com/licenses/upl.

package oracle.kubernetes.operator.helpers;

import static oracle.kubernetes.operator.KubernetesConstants.*;
import static oracle.kubernetes.operator.LabelConstants.*;
import static oracle.kubernetes.operator.VersionConstants.*;
import static oracle.kubernetes.operator.WebLogicConstants.*;
import static oracle.kubernetes.operator.utils.KubernetesArtifactUtils.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import com.meterware.simplestub.Memento;
import com.meterware.simplestub.StaticStubSupport;
import io.kubernetes.client.models.V1Container;
import io.kubernetes.client.models.V1ContainerPort;
import io.kubernetes.client.models.V1EnvFromSource;
import io.kubernetes.client.models.V1EnvVar;
import io.kubernetes.client.models.V1LocalObjectReference;
import io.kubernetes.client.models.V1ObjectMeta;
import io.kubernetes.client.models.V1PersistentVolumeClaimList;
import io.kubernetes.client.models.V1Pod;
import io.kubernetes.client.models.V1PodSpec;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import oracle.kubernetes.TestUtils;
import oracle.kubernetes.operator.ProcessingConstants;
import oracle.kubernetes.operator.TuningParameters.PodTuning;
import oracle.kubernetes.operator.wlsconfig.WlsServerConfig;
import oracle.kubernetes.operator.work.Component;
import oracle.kubernetes.operator.work.Packet;
import oracle.kubernetes.weblogic.domain.v1.Domain;
import oracle.kubernetes.weblogic.domain.v1.DomainSpec;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/** Tests PodHelper */
public class PodHelperTest {

  private List<Memento> mementos = new ArrayList<>();

  private static final String NAMESPACE = "test-namespace";
  private static final String DOMAIN_UID = "test-domain-uid";
  private static final String DOMAIN_NAME = "TestDomain";
  private static final String ADMIN_SERVER_NAME = "TestAdminServer";
  private static final String CLUSTER_NAME = "TestCluster";
  private static final String MANAGED_SERVER_NAME = "TestManagedServer";
  private static final String WEBLOGIC_CREDENTIALS_SECRET_NAME =
      "test-weblogic-credentials-secret-name";
  private static final int ADMIN_SERVER_PORT = 7654;
  private static final int MANAGED_SERVER_PORT = 4567;
  private static final String WEBLOGIC_DOMAIN_PERSISTENT_VOLUME_CLAIM_NAME =
      "test-weblogic-domain-pvc-name";
  private static final String INTERNAL_OPERATOR_CERT_FILE = "test-internal-operator-cert-file";

  private static final String RESTARTED_LABEL1 = "restartedLabel1";

  private static final String JAVA_OPTIONS = "JAVA_OPTIONS";
  private static final String ADMIN_STARTUP_MODE = "-Dweblogic.management.startupMode=ADMIN";

  private static final V1EnvVar ENV_VAR1 = newEnvVar("name1", "value1");
  private static final V1EnvVar ENV_VAR2 = newEnvVar("name2", "value2");
  private static final V1EnvVar ADMIN_STARTUP_MODE_ENV_VAR =
      newEnvVar(JAVA_OPTIONS, ADMIN_STARTUP_MODE);

  private static final DomainSpec DOMAIN_SPEC =
      newDomainSpec()
          .withDomainUID(DOMAIN_UID)
          .withDomainName(DOMAIN_NAME)
          .withAsName(ADMIN_SERVER_NAME)
          .withAsPort(ADMIN_SERVER_PORT)
          .withAdminSecret(newSecretReference().name(WEBLOGIC_CREDENTIALS_SECRET_NAME));

  private static Domain DOMAIN =
      newDomain().withSpec(DOMAIN_SPEC).withMetadata(newObjectMeta().namespace(NAMESPACE));

  private static final List<V1LocalObjectReference> IMAGE_PULL_SECRETS =
      newLocalObjectReferenceList()
          .addElement(newLocalObjectReference().name("weblogic-image-pull-secret-name"));

  private static final List<V1LocalObjectReference> NO_IMAGE_PULL_SECRETS = null;

  private static final V1PersistentVolumeClaimList NO_CLAIMS = newPersistentVolumeClaimList();

  private static final V1PersistentVolumeClaimList ONE_CLAIM =
      newPersistentVolumeClaimList()
          .addItemsItem(
              newPersistentVolumeClaim()
                  .metadata(newObjectMeta().name(WEBLOGIC_DOMAIN_PERSISTENT_VOLUME_CLAIM_NAME)));

  private static final String V1 = "V1";
  private static final String V2 = "V2";
  private static final String V3 = "V3";

  private static final String IMAGE1 = "image1";
  private static final String IMAGE2 = "image2";

  private static final String IMAGE_PULL_POLICY1 = ALWAYS_IMAGEPULLPOLICY;
  private static final String IMAGE_PULL_POLICY2 = NEVER_IMAGEPULLPOLICY;

  private static final List<V1LocalObjectReference> SECRET1 =
      newList(newLocalObjectReference().name("secret1"));
  private static final List<V1LocalObjectReference> SECRET2 =
      newList(newLocalObjectReference().name("secret2"));

  private static final List<V1ContainerPort> PORT1 =
      newList(newContainerPort().containerPort(1234).protocol("TCP"));
  private static final List<V1ContainerPort> PORT2 =
      newList(newContainerPort().containerPort(4321).protocol("TCP"));

  private static final List<V1EnvVar> ENV1 = newList(newEnvVar("env1", "val1"));
  private static final List<V1EnvVar> ENV2 = newList(newEnvVar("env2", "val2"));

  private static final List<V1EnvFromSource> ENV_FROM1 =
      newList(newEnvFromSource().secretRef(newSecretEnvSource().name("source1")));
  private static final List<V1EnvFromSource> ENV_FROM2 =
      newList(newEnvFromSource().secretRef(newSecretEnvSource().name("source2")));

  private static final String CONTAINER1_NAME = "container1";
  private static final String CONTAINER2_NAME = "container2";
  private static final String CONTAINER3_NAME = "container3";

  private static final V1Container CONTAINER1 = newContainer().name(CONTAINER1_NAME);
  private static final V1Container CONTAINER2 = newContainer().name(CONTAINER2_NAME);
  private static final V1Container CONTAINER3 = newContainer().name(CONTAINER3_NAME);

  @Before
  public void setUp() throws Exception {
    mementos.add(TestUtils.silenceOperatorLogger());
    mementos.add(
        StaticStubSupport.install(
            DomainPresenceInfoManager.class, "domains", new ConcurrentHashMap<>()));
    mementos.add(
        StaticStubSupport.install(
            ServerKubernetesObjectsManager.class, "serverMap", new ConcurrentHashMap<>()));
  }

  @After
  public void tearDown() throws Exception {
    for (Memento memento : mementos) memento.revert();
  }

  @Test
  public void validateCurrentPod_CurrentNullMetadata_returnsFalse() {
    V1Pod current = createPopulatedPod();
    current.setMetadata(null);
    assertThat(PodHelper.validateCurrentPod(createPopulatedPod(), current), equalTo(false));
  }

  @Test
  public void validateCurrentPod_differentVersion_returnsFalse() {
    V1Pod current = createPopulatedPod();
    current.getMetadata().putLabelsItem(RESOURCE_VERSION_LABEL, DOMAIN_V1);
    assertThat(PodHelper.validateCurrentPod(createPopulatedPod(), current), equalTo(false));
  }

  @Test
  public void validateCurrentPod_differentRestartedLabel_returnsFalse() {
    V1Pod build = createPopulatedPod();
    build.getMetadata().putLabelsItem(RESTARTED_LABEL, RESTARTED_LABEL1);
    assertThat(PodHelper.validateCurrentPod(build, createPopulatedPod()), equalTo(false));
  }

  @Test
  public void validateCurrentPod_differentImagePullSecrets_returnsFalse() {
    V1Pod current = createPopulatedPod();
    current.getSpec().imagePullSecrets(SECRET1);
    assertThat(PodHelper.validateCurrentPod(createPopulatedPod(), current), equalTo(false));
  }

  @Test
  public void validateCurrentPod_differentContainers_returnsFalse() {
    V1Pod current = createPopulatedPod();
    current.getSpec().addContainersItem(newContainer().name(CONTAINER2_NAME));
    assertThat(PodHelper.validateCurrentPod(createPopulatedPod(), current), equalTo(false));
  }

  @Test
  public void validateCurrentPod_samePods_returnsTrue() {
    assertThat(
        PodHelper.validateCurrentPod(createPopulatedPod(), createPopulatedPod()), equalTo(true));
  }

  private V1Pod createPopulatedPod() {
    return newPod()
        .metadata(newObjectMeta().putLabelsItem(RESOURCE_VERSION_LABEL, DOMAIN_V1DOT1))
        .spec(
            newPodSpec()
                .addContainersItem(
                    newContainer()
                        .name(CONTAINER1_NAME)
                        .image(IMAGE1)
                        .imagePullPolicy(IMAGE_PULL_POLICY1)));
  }

  @Test
  public void validateCurrentHasMetadata_nullMetadata_returnsFalse() {
    assertThat(PodHelper.validateCurrentHasMetadata(newPod()), equalTo(false));
  }

  @Test
  public void validateCurrentHasMetadata_hasMetadata_returnsTrue() {
    assertThat(
        PodHelper.validateCurrentHasMetadata(newPod().metadata(newObjectMeta())), equalTo(true));
  }

  @Test
  public void validateCurrentVersion_V1_returnsFalse() {
    assertThat(
        PodHelper.validateCurrentVersion(
            newPod().metadata(newObjectMeta().putLabelsItem(RESOURCE_VERSION_LABEL, DOMAIN_V1))),
        equalTo(false));
  }

  @Test
  public void validateCurrentVersion_V1DOT1_returnsTrue() {
    assertThat(
        PodHelper.validateCurrentVersion(
            newPod()
                .metadata(newObjectMeta().putLabelsItem(RESOURCE_VERSION_LABEL, DOMAIN_V1DOT1))),
        equalTo(true));
  }

  @Test
  public void validateCurrentRestartedLabel_buildNull_currentNull_returnsTrue() {
    assertThat(
        PodHelper.validateCurrentRestartedLabel(
            createPodWithoutRestartedLabel(), createPodWithoutRestartedLabel()),
        equalTo(true));
  }

  @Test
  public void validateCurrentRestartedLabel_buildNull_currentHasLabel_returnsTrue() {
    assertThat(
        PodHelper.validateCurrentRestartedLabel(
            createPodWithoutRestartedLabel(), createPodWithRestartedLabel(RESTARTED_LABEL1)),
        equalTo(true));
  }

  @Test
  public void validateCurrentRestartedLabel_buildHasLabel_currentNull_returnsFalse() {
    assertThat(
        PodHelper.validateCurrentRestartedLabel(
            createPodWithRestartedLabel(RESTARTED_LABEL1), createPodWithoutRestartedLabel()),
        equalTo(false));
  }

  @Test
  public void validateCurrentRestartedLabel_differentLabels_returnsFalse() {
    assertThat(
        PodHelper.validateCurrentRestartedLabel(
            createPodWithRestartedLabel("label1"), createPodWithRestartedLabel("label2")),
        equalTo(false));
  }

  @Test
  public void validateCurrentRestartedLabel_sameLabel_returnsTrue() {
    assertThat(
        PodHelper.validateCurrentRestartedLabel(
            createPodWithRestartedLabel(RESTARTED_LABEL1),
            createPodWithRestartedLabel(RESTARTED_LABEL1)),
        equalTo(true));
  }

  private V1Pod createPodWithRestartedLabel(String restartedLabel) {
    return newPod().metadata(newObjectMeta().putLabelsItem(RESTARTED_LABEL, restartedLabel));
  }

  private V1Pod createPodWithoutRestartedLabel() {
    return newPod().metadata(newObjectMeta());
  }

  @Test
  public void getRestartedLabel_nullLabels_returnsNull() {
    assertThat(
        PodHelper.getRestartedLabel(newPod().metadata(newObjectMeta().labels(null))), nullValue());
  }

  @Test
  public void getRestartedLabel_labelsDoesntIncludeRestartedLabel_returnsNull() {
    assertThat(
        PodHelper.getRestartedLabel(
            newPod().metadata(newObjectMeta().putLabelsItem("label1", "value1"))),
        nullValue());
  }

  @Test
  public void getRestartedLabel_labelsIncludesRestartedLabel_returnsRestartedLabel() {
    assertThat(
        PodHelper.getRestartedLabel(
            newPod().metadata(newObjectMeta().putLabelsItem(RESTARTED_LABEL, RESTARTED_LABEL1))),
        equalTo(RESTARTED_LABEL1));
  }

  @Test
  public void validateCurrentImagePullSecrets_sameSecrets_returnsTrue() {
    assertThat(
        PodHelper.validateCurrentImagePullSecrets(
            newPod().spec(newPodSpec().imagePullSecrets(SECRET1)),
            newPod().spec(newPodSpec().imagePullSecrets(SECRET1))),
        equalTo(true));
  }

  @Test
  public void validateCurrentImagePullSecrets_differentSecrets_returnsFalse() {
    assertThat(
        PodHelper.validateCurrentImagePullSecrets(
            newPod().spec(newPodSpec().imagePullSecrets(SECRET1)),
            newPod().spec(newPodSpec().imagePullSecrets(SECRET1))),
        equalTo(true));
  }

  @Test
  public void validateCurrentContainers_currentNullContainerList_returns_false() {
    assertThat(
        PodHelper.validateCurrentContainers(
            createPod(newList(createContainer(CONTAINER1_NAME, IMAGE1))), createPod(null)),
        equalTo(false));
  }

  @Test
  public void validateCurrentContainers_differentContainerCounts_returns_false() {
    assertThat(
        PodHelper.validateCurrentContainers(
            createPod(newList(createContainer(CONTAINER1_NAME, IMAGE1))),
            createPod(
                newList(
                    createContainer(CONTAINER1_NAME, IMAGE1),
                    createContainer(CONTAINER2_NAME, IMAGE2)))),
        equalTo(false));
  }

  @Test
  public void validateCurrentContainers_differentContainerNames_returns_false() {
    assertThat(
        PodHelper.validateCurrentContainers(
            createPod(newList(createContainer(CONTAINER1_NAME, IMAGE1))),
            createPod(newList(createContainer(CONTAINER2_NAME, IMAGE1)))),
        equalTo(false));
  }

  @Test
  public void
      validateCurrentContainers_sameContainersNamesDifferentContainerContents_returns_false() {
    assertThat(
        PodHelper.validateCurrentContainers(
            createPod(newList(createContainer(CONTAINER1_NAME, IMAGE1))),
            createPod(newList(createContainer(CONTAINER1_NAME, IMAGE2)))),
        equalTo(false));
  }

  @Test
  public void validateCurrentContainers_sameContainerNamesSameContainerContents_returns_true() {
    assertThat(
        PodHelper.validateCurrentContainers(
            createPod(newList(createContainer(CONTAINER1_NAME, IMAGE1))),
            createPod(newList(createContainer(CONTAINER1_NAME, IMAGE1)))),
        equalTo(true));
  }

  private V1Pod createPod(List<V1Container> containers) {
    return newPod().spec(newPodSpec().containers(containers));
  }

  private V1Container createContainer(String name, String image) {
    return createContainer(image, IMAGE_PULL_POLICY1, PORT1, ENV1, ENV_FROM1).name(name);
  }

  @Test
  public void findCurrentContainer_containerInList_returnsContainer() {
    assertThat(
        PodHelper.findCurrentContainer(CONTAINER1, newList(CONTAINER3, CONTAINER2, CONTAINER1)),
        equalTo(CONTAINER1));
  }

  @Test
  public void findCurrentContainer_containerNotInList_returnsNull() {
    assertThat(
        PodHelper.findCurrentContainer(CONTAINER1, newList(CONTAINER2, CONTAINER3)), nullValue());
  }

  @Test
  public void validateCurrentContainer_sameProperties_returnsTrue() {
    assertThat(
        PodHelper.validateCurrentContainer(
            createContainer(IMAGE1, IMAGE_PULL_POLICY1, PORT1, ENV1, ENV_FROM1),
            createContainer(IMAGE1, IMAGE_PULL_POLICY1, PORT1, ENV1, ENV_FROM1)),
        equalTo(true));
  }

  @Test
  public void validateCurrentContainer_differentImage_returnsFalse() {
    assertThat(
        PodHelper.validateCurrentContainer(
            createContainer(IMAGE1, IMAGE_PULL_POLICY1, PORT1, ENV1, ENV_FROM1),
            createContainer(IMAGE2, IMAGE_PULL_POLICY1, PORT1, ENV1, ENV_FROM1)),
        equalTo(false));
  }

  @Test
  public void validateCurrentContainer_differentImagePullPolicy_returnsFalse() {
    assertThat(
        PodHelper.validateCurrentContainer(
            createContainer(IMAGE1, IMAGE_PULL_POLICY1, PORT1, ENV1, ENV_FROM1),
            createContainer(IMAGE1, IMAGE_PULL_POLICY2, PORT1, ENV1, ENV_FROM1)),
        equalTo(false));
  }

  @Test
  public void validateCurrentContainer_differentPorts_returnsFalse() {
    assertThat(
        PodHelper.validateCurrentContainer(
            createContainer(IMAGE1, IMAGE_PULL_POLICY1, PORT1, ENV1, ENV_FROM1),
            createContainer(IMAGE1, IMAGE_PULL_POLICY1, PORT2, ENV1, ENV_FROM1)),
        equalTo(false));
  }

  @Test
  public void validateCurrentContainer_differentEnv_returnsFalse() {
    assertThat(
        PodHelper.validateCurrentContainer(
            createContainer(IMAGE1, IMAGE_PULL_POLICY1, PORT1, ENV1, ENV_FROM1),
            createContainer(IMAGE1, IMAGE_PULL_POLICY1, PORT1, ENV2, ENV_FROM1)),
        equalTo(false));
  }

  @Test
  public void validateCurrentContainer_differentEnvFrom_returnsFalse() {
    assertThat(
        PodHelper.validateCurrentContainer(
            createContainer(IMAGE1, IMAGE_PULL_POLICY1, PORT1, ENV1, ENV_FROM1),
            createContainer(IMAGE1, IMAGE_PULL_POLICY1, PORT1, ENV1, ENV_FROM2)),
        equalTo(false));
  }

  private V1Container createContainer(
      String image,
      String imagePullPolicy,
      List<V1ContainerPort> ports,
      List<V1EnvVar> env,
      List<V1EnvFromSource> envFrom) {
    return newContainer()
        .image(image)
        .imagePullPolicy(imagePullPolicy)
        .ports(ports)
        .env(env)
        .envFrom(envFrom);
  }

  @Test
  public void compareUnordered_null_null_returnsTrue() {
    assertThat(PodHelper.compareUnordered(null, null), equalTo(true));
  }

  @Test
  public void compareUnordered_null_notNull_returnsFalse() {
    assertThat(PodHelper.compareUnordered(null, newList(V1)), equalTo(false));
  }

  @Test
  public void compareUnordered_notNull_null_returnsFalse() {
    assertThat(PodHelper.compareUnordered(newList(V1), null), equalTo(false));
  }

  @Test
  public void compareUnordered_differentSizes_returnsFalse() {
    assertThat(PodHelper.compareUnordered(newList(V1), newList(V1, V2)), equalTo(false));
  }

  @Test
  public void compareUnordered_elementsUnique_sameElements_sameOrder_returnsTrue() {
    assertThat(PodHelper.compareUnordered(newList(V1, V2, V3), newList(V1, V2, V3)), equalTo(true));
  }

  @Test
  public void compareUnordered_elementsUnique_sameElements_differentOrder_returnsTrue() {
    assertThat(PodHelper.compareUnordered(newList(V1, V2, V3), newList(V2, V1, V3)), equalTo(true));
  }

  @Test
  public void compareUnordered_duplicateElements_differentElements_returnsFalse() {
    assertThat(
        PodHelper.compareUnordered(newList(V1, V1, V2), newList(V1, V3, V1)), equalTo(false));
  }

  @Test
  public void compareUnordered_duplicateElements_sameElements_differentOrder_returnsTrue() {
    assertThat(PodHelper.compareUnordered(newList(V1, V1, V2), newList(V1, V2, V1)), equalTo(true));
  }

  private static <T> List<T> newList(T... elements) {
    List<T> list = new ArrayList();
    for (T element : elements) {
      list.add(element);
    }
    return list;
  }

  @Test
  public void computeAdminPodConfig_clusteredSerer_addsCorrectResources() {
    ClusteredServerConfig serverConfig =
        (new ClusteredServerConfig())
            .withClusterName(CLUSTER_NAME)
            .withServerName(ADMIN_SERVER_NAME);
    computeAdminPodConfig_addsCorrectResources(serverConfig);
  }

  @Test
  public void computeAdminPodConfig_nonClusteredServer_addsCorrectResources() {
    NonClusteredServerConfig serverConfig =
        (new NonClusteredServerConfig()).withServerName(ADMIN_SERVER_NAME);
    computeAdminPodConfig_addsCorrectResources(serverConfig);
  }

  private void computeAdminPodConfig_addsCorrectResources(ServerConfig serverConfig) {
    setupBaseServerConfig(serverConfig);
    Packet packet = newPacket(ONE_CLAIM);
    V1Pod actual =
        PodHelper.computeAdminPodConfig(
            serverConfig, newPodTuning(), INTERNAL_OPERATOR_CERT_FILE, packet);

    V1Pod want = getExpectedBaseServerPod(serverConfig, ADMIN_SERVER_PORT, ONE_CLAIM);
    V1Container containerWant = want.getSpec().getContainers().get(0);
    containerWant.addEnvItem(newEnvVar("INTERNAL_OPERATOR_CERT", INTERNAL_OPERATOR_CERT_FILE));
    want.getSpec().setHostname(want.getMetadata().getName());

    assertThat(actual, equalTo(want));
  }

  @Test
  public void computeManagedPodConfig_clusteredServer_addsCorrectResources() {
    ClusteredServerConfig serverConfig =
        (new ClusteredServerConfig())
            .withClusterName(CLUSTER_NAME)
            .withServerName(MANAGED_SERVER_NAME);
    computeManagedPodConfig_addsCorrectResources(serverConfig);
  }

  @Test
  public void computeManagedPodConfig_nonClusteredServer_addsCorrectResources() {
    NonClusteredServerConfig serverConfig =
        (new NonClusteredServerConfig()).withServerName(MANAGED_SERVER_NAME);
    computeManagedPodConfig_addsCorrectResources(serverConfig);
  }

  private void computeManagedPodConfig_addsCorrectResources(ServerConfig serverConfig) {
    setupBaseServerConfig(serverConfig);
    Packet packet = newPacket(NO_CLAIMS);
    packet.put(
        ProcessingConstants.SERVER_SCAN,
        // no cluster name, listen address, no network access points since PodHelper doesn't use
        // them:
        new WlsServerConfig(
            serverConfig.getServerName(),
            null,
            MANAGED_SERVER_PORT,
            null,
            null,
            false,
            null,
            null));
    V1Pod actual = PodHelper.computeManagedPodConfig(serverConfig, newPodTuning(), packet);

    V1Pod want = getExpectedBaseServerPod(serverConfig, MANAGED_SERVER_PORT, NO_CLAIMS);
    V1Container containerWant = want.getSpec().getContainers().get(0);
    containerWant.addCommandItem(ADMIN_SERVER_NAME).addCommandItem("" + ADMIN_SERVER_PORT);

    assertThat(actual, equalTo(want));
  }

  @Test
  public void computeBaseServerPodConfig_addsCorrectResources() {
    ServerConfig serverConfig = (new ServerConfig()).withServerName(MANAGED_SERVER_NAME);
    setupBaseServerConfig(serverConfig);
    Packet packet = newPacket(ONE_CLAIM);
    V1Pod actual =
        PodHelper.computeBaseServerPodConfig(
            serverConfig, MANAGED_SERVER_PORT, newPodTuning(), packet);

    V1Pod want = getExpectedBaseServerPod(serverConfig, MANAGED_SERVER_PORT, ONE_CLAIM);

    assertThat(actual, equalTo(want));
  }

  private Packet newPacket(V1PersistentVolumeClaimList claims) {
    DomainPresenceInfo info = DomainPresenceInfoManager.getOrCreate(DOMAIN);
    info.setClaims(claims);
    Packet packet = new Packet();
    packet
        .getComponents()
        .put(ProcessingConstants.DOMAIN_COMPONENT_NAME, Component.createFor(info));
    return packet;
  }

  private void setupBaseServerConfig(ServerConfig serverConfig) {
    serverConfig
        .withImage(DEFAULT_IMAGE)
        .withImagePullPolicy(IFNOTPRESENT_IMAGEPULLPOLICY)
        .withImagePullSecrets(null)
        .withRestartedLabel(null)
        .withEnv(null); // TBD
  }

  private V1Pod getExpectedBaseServerPod(
      ServerConfig serverConfig, int weblogicServerPort, V1PersistentVolumeClaimList claims) {
    V1Container container = newContainer().name("weblogic-server");
    V1PodSpec podSpec = newPodSpec().addContainersItem(container);

    addExpectedImageProperties(container, serverConfig);
    addExpectedImageProperties(podSpec, serverConfig);
    addExpectedWeblogicServerPort(container, weblogicServerPort);
    addExpectedCommands(container, serverConfig);
    addExpectedVolumes(podSpec, claims);
    addExpectedVolumeMounts(container);
    addExpectedWeblogicEnvVars(container, serverConfig);

    V1Pod pod =
        newPod()
            .spec(podSpec)
            .metadata(getExpectedServerPodMetadata(serverConfig, weblogicServerPort));

    return pod;
  }

  @Test
  public void setWeblogicServerImage_addsCorrectResources() {
    ServerConfig serverConfig =
        (new ServerConfig())
            .withImage(DEFAULT_IMAGE)
            .withImagePullPolicy(ALWAYS_IMAGEPULLPOLICY)
            .withImagePullSecrets(IMAGE_PULL_SECRETS);
    V1PodSpec actualPodSpec = newPodSpec();
    V1Container actualContainer = newContainer();
    PodHelper.setWeblogicServerImage(actualPodSpec, actualContainer, serverConfig);

    V1PodSpec podSpecWant = newPodSpec();
    addExpectedImageProperties(podSpecWant, serverConfig);
    assertThat(actualPodSpec, equalTo(podSpecWant));

    V1Container containerWant = newContainer();
    addExpectedImageProperties(containerWant, serverConfig);
    assertThat(actualContainer, equalTo(containerWant));
  }

  private void addExpectedImageProperties(V1Container container, ServerConfig serverConfig) {
    container.image(serverConfig.getImage()).imagePullPolicy(serverConfig.getImagePullPolicy());
  }

  private void addExpectedImageProperties(V1PodSpec podSpec, ServerConfig serverConfig) {
    podSpec.imagePullSecrets(serverConfig.getImagePullSecrets());
  }

  @Test
  public void addWeblogicServerPort_addsCorrectResources() {
    V1Container actual = newContainer();
    PodHelper.addWeblogicServerPort(actual, MANAGED_SERVER_PORT);

    V1Container want = newContainer();
    addExpectedWeblogicServerPort(want, MANAGED_SERVER_PORT);

    assertThat(actual, equalTo(want));
  }

  private void addExpectedWeblogicServerPort(V1Container container, int weblogicServerPort) {
    container.addPortsItem(newContainerPort().containerPort(weblogicServerPort).protocol("TCP"));
  }

  @Test
  public void addCommands() {
    ServerConfig serverConfig = (new ServerConfig()).withServerName(MANAGED_SERVER_NAME);
    V1Container actual = newContainer();
    PodHelper.addCommands(actual, DOMAIN_SPEC, serverConfig, newPodTuning());

    V1Container want = newContainer();
    addExpectedCommands(want, serverConfig);

    assertThat(actual, equalTo(want));
  }

  private void addExpectedCommands(V1Container container, ServerConfig serverConfig) {
    addExpectedServerStartCommand(container, serverConfig);
    addExpectedPreStopHandler(container, serverConfig);
    addExpectedLivenessProbe(container, serverConfig);
    addExpectedReadinessProbe(container, serverConfig);
  }

  @Test
  public void addServerStartCommand_addsCorrectCommand() {
    ServerConfig serverConfig = (new ServerConfig()).withServerName(MANAGED_SERVER_NAME);
    V1Container actual = newContainer();
    PodHelper.addServerStartCommand(actual, DOMAIN_SPEC, serverConfig);

    V1Container want = newContainer();
    addExpectedServerStartCommand(want, serverConfig);

    assertThat(actual, equalTo(want));
  }

  private void addExpectedServerStartCommand(V1Container container, ServerConfig serverConfig) {
    container
        .addCommandItem("/weblogic-operator/scripts/startServer.sh")
        .addCommandItem(DOMAIN_UID)
        .addCommandItem(serverConfig.getServerName())
        .addCommandItem(DOMAIN_NAME);
  }

  @Test
  public void addPreStopHandler_addsCorrectHandler() {
    ServerConfig serverConfig = (new ServerConfig()).withServerName(MANAGED_SERVER_NAME);
    V1Container actual = newContainer();
    PodHelper.addPreStopHandler(actual, DOMAIN_SPEC, serverConfig);

    V1Container want = newContainer();
    addExpectedPreStopHandler(want, serverConfig);

    assertThat(actual, equalTo(want));
  }

  private void addExpectedPreStopHandler(V1Container container, ServerConfig serverConfig) {
    container.lifecycle(
        newLifecycle()
            .preStop(
                newHandler()
                    .exec(
                        newExecAction()
                            .addCommandItem("/weblogic-operator/scripts/stopServer.sh")
                            .addCommandItem(DOMAIN_UID)
                            .addCommandItem(serverConfig.getServerName())
                            .addCommandItem(DOMAIN_NAME))));
  }

  @Test
  public void addLivenessProbe_addsCorrectProbe() {
    ServerConfig serverConfig = (new ServerConfig()).withServerName(MANAGED_SERVER_NAME);
    V1Container actual = newContainer();
    PodHelper.addLivenessProbe(actual, DOMAIN_SPEC, serverConfig, newPodTuning());

    V1Container want = newContainer();
    addExpectedLivenessProbe(want, serverConfig);

    assertThat(actual, equalTo(want));
  }

  private void addExpectedLivenessProbe(V1Container container, ServerConfig serverConfig) {
    container.livenessProbe(
        newProbe()
            .initialDelaySeconds(10)
            .periodSeconds(10)
            .timeoutSeconds(5)
            .failureThreshold(1)
            .exec(
                newExecAction()
                    .addCommandItem("/weblogic-operator/scripts/livenessProbe.sh")
                    .addCommandItem(DOMAIN_NAME)
                    .addCommandItem(serverConfig.getServerName())));
  }

  @Test
  public void addReadinessProbe_addsCorrectProbe() {
    ServerConfig serverConfig = (new ServerConfig()).withServerName(MANAGED_SERVER_NAME);
    V1Container actual = newContainer();
    PodHelper.addReadinessProbe(actual, DOMAIN_SPEC, serverConfig, newPodTuning());

    V1Container want = newContainer();
    addExpectedReadinessProbe(want, serverConfig);

    assertThat(actual, equalTo(want));
  }

  private void addExpectedReadinessProbe(V1Container container, ServerConfig serverConfig) {
    container.readinessProbe(
        newProbe()
            .initialDelaySeconds(2)
            .periodSeconds(10)
            .timeoutSeconds(5)
            .failureThreshold(1)
            .exec(
                newExecAction()
                    .addCommandItem("/weblogic-operator/scripts/readinessProbe.sh")
                    .addCommandItem(DOMAIN_NAME)
                    .addCommandItem(serverConfig.getServerName())));
  }

  private PodTuning newPodTuning() {
    return new PodTuning(
        /* "readinessProbeInitialDelaySeconds" */ 2,
        /* "readinessProbeTimeoutSeconds" */ 5,
        /* "readinessProbePeriodSeconds" */ 10,
        /* "livenessProbeInitialDelaySeconds" */ 10,
        /* "livenessProbeTimeoutSeconds" */ 5,
        /* "livenessProbePeriodSeconds" */ 10);
  }

  @Test
  public void addVolumes_addsCorrectVolumesAndMounts() {
    V1PodSpec actual = newPodSpec();
    PodHelper.addVolumes(actual, DOMAIN_SPEC, ONE_CLAIM);

    V1PodSpec want = newPodSpec();
    addExpectedVolumes(want, ONE_CLAIM);

    assertThat(actual, equalTo(want));
  }

  private void addExpectedVolumes(V1PodSpec podSpec, V1PersistentVolumeClaimList claims) {
    if (!claims.getItems().isEmpty()) {
      addExpectedWeblogicDomainStorageVolume(podSpec);
    }
    addExpectedWeblogicCredentialsVolume(podSpec);
    addExpectedWeblogicDomainConfigMapVolume(podSpec);
  }

  @Test
  public void addWeblogicDomainStorageVolume_oneClaim_addsVolume() {
    V1PodSpec actual = newPodSpec();
    PodHelper.addWeblogicDomainStorageVolume(actual, ONE_CLAIM);

    V1PodSpec want = newPodSpec();
    addExpectedWeblogicDomainStorageVolume(want);

    assertThat(actual, equalTo(want));
  }

  @Test
  public void addWeblogicDomainStorageVolume_noClaims_doesntAddVolume() {
    V1PodSpec actual = newPodSpec();
    PodHelper.addWeblogicDomainStorageVolume(actual, NO_CLAIMS);

    V1PodSpec want = newPodSpec();

    assertThat(actual, equalTo(want));
  }

  private void addExpectedWeblogicDomainStorageVolume(V1PodSpec podSpec) {
    podSpec.addVolumesItem(
        newVolume()
            .name("weblogic-domain-storage-volume")
            .persistentVolumeClaim(
                newPersistentVolumeClaimVolumeSource()
                    .claimName(WEBLOGIC_DOMAIN_PERSISTENT_VOLUME_CLAIM_NAME)));
  }

  @Test
  public void addWeblogicCredentialsVolume_addsVolume() {
    V1PodSpec actual = newPodSpec();
    PodHelper.addWeblogicCredentialsVolume(actual, DOMAIN_SPEC);

    V1PodSpec want = newPodSpec();
    addExpectedWeblogicCredentialsVolume(want);

    assertThat(actual, equalTo(want));
  }

  private void addExpectedWeblogicCredentialsVolume(V1PodSpec podSpec) {
    podSpec.addVolumesItem(
        newVolume()
            .name("weblogic-credentials-volume")
            .secret(newSecretVolumeSource().secretName(WEBLOGIC_CREDENTIALS_SECRET_NAME)));
  }

  @Test
  public void addWeblogicDomainConfigMapVolume_addsVolume() {
    V1PodSpec actual = newPodSpec();
    PodHelper.addWeblogicDomainConfigMapVolume(actual);

    V1PodSpec want = newPodSpec();
    addExpectedWeblogicDomainConfigMapVolume(want);

    assertThat(actual, equalTo(want));
  }

  private void addExpectedWeblogicDomainConfigMapVolume(V1PodSpec podSpec) {
    podSpec.addVolumesItem(
        newVolume()
            .name("weblogic-domain-cm-volume")
            .configMap(newConfigMapVolumeSource().name("weblogic-domain-cm").defaultMode(0555)));
  }

  @Test
  public void addVolumesMounts_addsCorrectVolumeMounts() {
    V1Container actual = newContainer();
    PodHelper.addVolumeMounts(actual);

    V1Container want = newContainer();
    addExpectedVolumeMounts(want);

    assertThat(actual, equalTo(want));
  }

  private void addExpectedVolumeMounts(V1Container container) {
    addExpectedWeblogicDomainStorageVolumeMount(container);
    addExpectedWeblogicCredentialsVolumeMount(container);
    addExpectedWeblogicDomainConfigMapVolumeMount(container);
  }

  @Test
  public void addWeblogicDomainStorageVolumeMount_addsVolumeMount() {
    V1Container actual = newContainer();
    PodHelper.addWeblogicDomainStorageVolumeMount(actual);

    V1Container want = newContainer();
    addExpectedWeblogicDomainStorageVolumeMount(want);

    assertThat(actual, equalTo(want));
  }

  private void addExpectedWeblogicDomainStorageVolumeMount(V1Container container) {
    container.addVolumeMountsItem(
        newVolumeMount().name("weblogic-domain-storage-volume").mountPath("/shared"));
  }

  @Test
  public void addWeblogicCredentialsVolumeMount_addsVolumeMount() {
    V1Container actual = newContainer();
    PodHelper.addWeblogicCredentialsVolumeMount(actual);

    V1Container want = newContainer();
    addExpectedWeblogicCredentialsVolumeMount(want);

    assertThat(actual, equalTo(want));
  }

  private void addExpectedWeblogicCredentialsVolumeMount(V1Container container) {
    container.addVolumeMountsItem(
        newVolumeMount()
            .name("weblogic-credentials-volume")
            .mountPath("/weblogic-operator/secrets")
            .readOnly(true));
  }

  @Test
  public void addWeblogicDomainConfigMapVolumeMount_addsVolumeMount() {
    V1Container actual = newContainer();
    PodHelper.addWeblogicDomainConfigMapVolumeMount(actual);

    V1Container want = newContainer();
    addExpectedWeblogicDomainConfigMapVolumeMount(want);

    assertThat(actual, equalTo(want));
  }

  private void addExpectedWeblogicDomainConfigMapVolumeMount(V1Container container) {
    container.addVolumeMountsItem(
        newVolumeMount()
            .name("weblogic-domain-cm-volume")
            .mountPath("/weblogic-operator/scripts")
            .readOnly(true));
  }

  @Test
  public void addWeblogicServerEnv_addsCorrectEnv() {
    ServerConfig serverConfig =
        (new ServerConfig())
            .withServerName(MANAGED_SERVER_NAME)
            .withStartedServerState(ADMIN_STATE)
            .withEnv(newEnvVarList().addElement(ENV_VAR1));
    V1Container actual = newContainer();
    PodHelper.addWeblogicServerEnv(actual, DOMAIN_SPEC, serverConfig);

    V1Container want = newContainer().addEnvItem(ENV_VAR1).addEnvItem(ADMIN_STARTUP_MODE_ENV_VAR);
    addExpectedWeblogicEnvVars(want, serverConfig);

    assertThat(actual, equalTo(want));
  }

  @Test
  public void createWeblogicServerEnv_adminMode_addsAdminModeToEnv() {
    assertThat(
        PodHelper.getWeblogicServerEnv(
            (new ServerConfig())
                .withEnv(newEnvVarList().addElement(ENV_VAR1))
                .withStartedServerState(ADMIN_STATE)),
        equalTo(newEnvVarList().addElement(ENV_VAR1).addElement(ADMIN_STARTUP_MODE_ENV_VAR)));
  }

  @Test
  public void createWeblogicServerEnv_runningMode_doesntAddAdminModeToEnv() {
    // We didn't set the env, and we didn't set admin mode, so we should get back a null env
    assertThat(
        PodHelper.getWeblogicServerEnv(new ServerConfig().withStartedServerState(RUNNING_STATE)),
        nullValue());
  }

  @Test
  public void createStartInAdminModeEnv_nullEnv_returnsCorrectEnv() {
    assertThat(
        PodHelper.createStartInAdminModeEnv(null),
        equalTo(newEnvVarList().addElement(ADMIN_STARTUP_MODE_ENV_VAR)));
  }

  @Test
  public void createStartInAdminModeEnv_envWithoutJavaOptions_returnsCorrectEnv() {
    List<V1EnvVar> env = newEnvVarList().addElement(ENV_VAR1);
    List<V1EnvVar> actual = PodHelper.createStartInAdminModeEnv(env);

    List<V1EnvVar> want =
        newEnvVarList().addElement(ENV_VAR1).addElement(ADMIN_STARTUP_MODE_ENV_VAR);

    assertThat(actual, equalTo(want));
  }

  @Test
  public void createStartInAdminModeEnv_envWithJavaOptions_returnsCorrectEnv() {
    String oldJavaOptions = "oldJavaOptions";
    List<V1EnvVar> env =
        newEnvVarList()
            .addElement(ENV_VAR1)
            .addElement(newEnvVar(JAVA_OPTIONS, oldJavaOptions))
            .addElement(ENV_VAR2);
    List<V1EnvVar> actual = PodHelper.createStartInAdminModeEnv(env);

    List<V1EnvVar> want =
        newEnvVarList()
            .addElement(ENV_VAR1)
            .addElement(newEnvVar(JAVA_OPTIONS, ADMIN_STARTUP_MODE + " " + oldJavaOptions))
            .addElement(ENV_VAR2);

    assertThat(actual, equalTo(want));
  }

  @Test(expected = IllegalStateException.class)
  public void
      createStartInAdminModeEnv_envHasJavaOptionsWithFromValue_throwsIllegalStateException() {
    PodHelper.createStartInAdminModeEnv(
        newEnvVarList().addElement(newEnvVar().name(JAVA_OPTIONS).valueFrom(newEnvVarSource())));
  }

  @Test
  public void newAdminStartupModeJavaOptionsEnvVar_returnsCorrectValue() {
    assertThat(
        PodHelper.newAdminStartupModeJavaOptionsEnvVar(null), equalTo(ADMIN_STARTUP_MODE_ENV_VAR));
  }

  @Test
  public void adminStartupModeJavaOptions_havePreviousValue_prependsAdminStartupMode() {
    String previousJavaOptions = "BLAH";
    assertThat(
        PodHelper.adminStartupModeJavaOptions(previousJavaOptions),
        equalTo(ADMIN_STARTUP_MODE + " " + previousJavaOptions));
  }

  @Test
  public void adminStartupModeJavaOptions_nullPreviousValue_returnsAdminStartupMode() {
    assertThat(PodHelper.adminStartupModeJavaOptions(null), equalTo(ADMIN_STARTUP_MODE));
  }

  @Test
  public void addWeblogicServerPodMetadata_addsCorrectMetaData() {
    ServerConfig serverConfig =
        (new ClusteredServerConfig())
            .withClusterName(CLUSTER_NAME)
            .withServerName(MANAGED_SERVER_NAME)
            .withRestartedLabel(RESTARTED_LABEL1);
    V1Pod actual = newPod();
    PodHelper.addWeblogicServerPodMetadata(actual, DOMAIN, serverConfig, MANAGED_SERVER_PORT);

    V1Pod want = newPod().metadata(getExpectedServerPodMetadata(serverConfig, MANAGED_SERVER_PORT));

    assertThat(actual, equalTo(want));
  }

  private V1ObjectMeta getExpectedServerPodMetadata(
      ServerConfig serverConfig, int weblogicServerPort) {
    V1ObjectMeta metadata =
        newObjectMeta()
            .name(DOMAIN_UID + "-" + serverConfig.getServerName().toLowerCase())
            .namespace(NAMESPACE)
            .putAnnotationsItem("prometheus.io/path", "/wls-exporter/metrics")
            .putAnnotationsItem("prometheus.io/port", "" + weblogicServerPort)
            .putAnnotationsItem("prometheus.io/scrape", "true")
            .putLabelsItem(RESOURCE_VERSION_LABEL, DOMAIN_V1DOT1)
            .putLabelsItem(CREATEDBYOPERATOR_LABEL, "true")
            .putLabelsItem(DOMAINNAME_LABEL, DOMAIN_NAME)
            .putLabelsItem(DOMAINUID_LABEL, DOMAIN_UID)
            .putLabelsItem(SERVERNAME_LABEL, serverConfig.getServerName());
    addExpectedClusterNameLabel(metadata, serverConfig);
    addExpectedRestartedLabel(metadata, serverConfig);
    return metadata;
  }

  @Test
  public void overrideContainerWeblogicEnvVars_addsEnvVars() {
    ServerConfig serverConfig = (new ServerConfig()).withServerName(MANAGED_SERVER_NAME);
    V1Container actual = newContainer();
    PodHelper.overrideContainerWeblogicEnvVars(actual, DOMAIN_SPEC, serverConfig);

    V1Container want = newContainer();
    addExpectedWeblogicEnvVars(want, serverConfig);

    assertThat(actual, equalTo(want));
  }

  private void addExpectedWeblogicEnvVars(V1Container container, ServerConfig serverConfig) {
    container
        .addEnvItem(newEnvVar("DOMAIN_NAME", DOMAIN_NAME))
        .addEnvItem(newEnvVar("DOMAIN_HOME", "/shared/domain/" + DOMAIN_NAME))
        .addEnvItem(newEnvVar("ADMIN_NAME", ADMIN_SERVER_NAME))
        .addEnvItem(newEnvVar("ADMIN_PORT", "" + ADMIN_SERVER_PORT))
        .addEnvItem(newEnvVar("SERVER_NAME", serverConfig.getServerName()))
        .addEnvItem(newEnvVar("ADMIN_USERNAME", null))
        .addEnvItem(newEnvVar("ADMIN_PASSWORD", null));
  }

  @Test
  public void addRestartedLabel_haveRestartedLabel_addsLabel() {
    ServerConfig serverConfig = (new ServerConfig()).withRestartedLabel(RESTARTED_LABEL1);
    V1ObjectMeta actual = newObjectMeta();
    PodHelper.addRestartedLabel(actual, serverConfig);

    V1ObjectMeta want = newObjectMeta();
    addExpectedRestartedLabel(want, serverConfig);

    assertThat(actual, equalTo(want));
  }

  private void addExpectedRestartedLabel(V1ObjectMeta metadata, ServerConfig serverConfig) {
    String restartedLabel = serverConfig.getRestartedLabel();
    if (restartedLabel != null) {
      metadata.putLabelsItem(RESTARTED_LABEL, restartedLabel);
    }
  }

  @Test
  public void addRestartedLabel_nullRestartedLabel_addsLabel() {
    ServerConfig serverConfig = new ServerConfig();
    V1ObjectMeta actual = newObjectMeta();
    PodHelper.addRestartedLabel(actual, serverConfig);

    V1ObjectMeta want = newObjectMeta();

    assertThat(actual, equalTo(want));
  }

  @Test
  public void addClusterNameLabel_clusteredServer_haveClusterName_addsLabel() {
    ServerConfig serverConfig = (new ClusteredServerConfig()).withClusterName(CLUSTER_NAME);
    V1ObjectMeta actual = newObjectMeta();
    PodHelper.addClusterNameLabel(actual, serverConfig);

    V1ObjectMeta want = newObjectMeta();
    addExpectedClusterNameLabel(want, serverConfig);

    assertThat(actual, equalTo(want));
  }

  @Test(expected = AssertionError.class)
  public void addClusterNameLabel_clusteredServer_nullClusterName_throwsAssertionError() {
    ServerConfig serverConfig = new ClusteredServerConfig();
    V1ObjectMeta actual = newObjectMeta();
    PodHelper.addClusterNameLabel(actual, serverConfig);
  }

  @Test
  public void addClusterNameLabel_nonClusteredServer_doesntAddLabel() {
    ServerConfig serverConfig = new NonClusteredServerConfig();
    V1ObjectMeta actual = newObjectMeta();
    PodHelper.addClusterNameLabel(actual, serverConfig);

    V1ObjectMeta want = newObjectMeta();

    assertThat(actual, equalTo(want));
  }

  private void addExpectedClusterNameLabel(V1ObjectMeta metadata, ServerConfig serverConfig) {
    if (serverConfig instanceof ClusteredServerConfig) {
      String clusterName = ((ClusteredServerConfig) serverConfig).getClusterName();
      metadata.putLabelsItem(CLUSTERNAME_LABEL, clusterName);
    }
  }
}
