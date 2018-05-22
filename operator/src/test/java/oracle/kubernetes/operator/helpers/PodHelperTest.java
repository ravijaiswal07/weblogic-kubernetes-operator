// Copyright 2018, Oracle Corporation and/or its affiliates.  All rights reserved.
// Licensed under the Universal Permissive License v 1.0 as shown at
// http://oss.oracle.com/licenses/upl.

package oracle.kubernetes.operator.helpers;

import static oracle.kubernetes.operator.KubernetesConstants.*;
import static oracle.kubernetes.operator.LabelConstants.*;
import static oracle.kubernetes.operator.VersionConstants.*;
import static oracle.kubernetes.operator.WebLogicConstants.*;
import static oracle.kubernetes.operator.utils.KubernetesArtifactUtils.*;
import static oracle.kubernetes.operator.utils.YamlUtils.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import com.meterware.simplestub.Memento;
import com.meterware.simplestub.StaticStubSupport;
import io.kubernetes.client.models.V1Container;
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
  private static final String CUSTOM_LATEST_IMAGE = "custom-image:latest";

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

  private static final V1PersistentVolumeClaimList NO_CLAIMS = newPersistentVolumeClaimList();

  private static final V1PersistentVolumeClaimList ONE_CLAIM =
      newPersistentVolumeClaimList()
          .addItemsItem(
              newPersistentVolumeClaim()
                  .metadata(newObjectMeta().name(WEBLOGIC_DOMAIN_PERSISTENT_VOLUME_CLAIM_NAME)));

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

  /*
    @Test
    public void computedAdminServerPodConfigForDefaults_isCorrect() throws Exception {
      assertThat(
          getActualAdminServerPodConfigForDefaults(),
          yamlEqualTo(getDesiredAdminServerPodConfigForDefaults()));
    }

    private V1Pod getDesiredAdminServerPodConfigForDefaults() {
      return getDesiredAdminServerPodConfigForDefaults(DEFAULT_IMAGE, IFNOTPRESENT_IMAGEPULLPOLICY);
    }

    private V1Pod getActualAdminServerPodConfigForDefaults() throws Exception {
      return getActualAdminServerPodConfig(
          getDomainCustomResourceForDefaults(null, null), // default image & image pull policy
          newEmptyPersistentVolumeClaimList());
    }

    @Test
    public void computedManagedServerPodConfigForDefaults_isCorrect() throws Exception {
      assertThat(
          getActualManagedServerPodConfigForDefaults(),
          yamlEqualTo(getDesiredManagedServerPodConfigForDefaults()));
    }

    private V1Pod getDesiredManagedServerPodConfigForDefaults() {
      return getDesiredManagedServerPodConfigForDefaults(DEFAULT_IMAGE, IFNOTPRESENT_IMAGEPULLPOLICY);
    }

    private V1Pod getActualManagedServerPodConfigForDefaults() throws Exception {
      return getActualManagedServerPodConfig(
          getDomainCustomResourceForDefaults(null, null), // default image & image pull policy
          newEmptyPersistentVolumeClaimList());
    }

    @Test
    public void computedAdminServerPodConfigForCustomLatestImageAndDefaults_isCorrect()
        throws Exception {
      assertThat(
          getActualAdminServerPodConfigForCustomLatestImageAndDefaults(),
          yamlEqualTo(getDesiredAdminServerPodConfigForCustomLatestImageAndDefaults()));
    }

    private V1Pod getDesiredAdminServerPodConfigForCustomLatestImageAndDefaults() {
      return getDesiredAdminServerPodConfigForDefaults(CUSTOM_LATEST_IMAGE, ALWAYS_IMAGEPULLPOLICY);
    }

    private V1Pod getActualAdminServerPodConfigForCustomLatestImageAndDefaults() throws Exception {
      return getActualAdminServerPodConfig(
          getDomainCustomResourceForDefaults(
              CUSTOM_LATEST_IMAGE, null), // custom latest image & default image pull policy
          newEmptyPersistentVolumeClaimList());
    }

    @Test
    public void computedManagedServerPodConfigForCustomLatestImageAndDefaults_isCorrect()
        throws Exception {
      assertThat(
          getActualManagedServerPodConfigForCustomLatestImageAndDefaults(),
          yamlEqualTo(getDesiredManagedServerPodConfigForCustomLatestImageAndDefaults()));
    }

    private V1Pod getDesiredManagedServerPodConfigForCustomLatestImageAndDefaults() {
      return getDesiredManagedServerPodConfigForDefaults(CUSTOM_LATEST_IMAGE, ALWAYS_IMAGEPULLPOLICY);
    }

    private V1Pod getActualManagedServerPodConfigForCustomLatestImageAndDefaults() throws Exception {
      return getActualManagedServerPodConfig(
          getDomainCustomResourceForDefaults(
              CUSTOM_LATEST_IMAGE, null), // custom latest image & default image pull policy
          newEmptyPersistentVolumeClaimList());
    }

    @Test
    public void
        computedAdminServerPodConfigForCustomLatestImageAndCustomImagePullPolicyAndDefaults_isCorrect()
            throws Exception {
      assertThat(
          getActualAdminServerPodConfigForCustomLatestImageAndCustomImagePullPolicyAndDefaults(),
          yamlEqualTo(
              getDesiredAdminServerPodConfigForCustomLatestImageAndCustomImagePullPolicyAndDefaults()));
    }

    private V1Pod
        getDesiredAdminServerPodConfigForCustomLatestImageAndCustomImagePullPolicyAndDefaults() {
      return getDesiredAdminServerPodConfigForDefaults(
          CUSTOM_LATEST_IMAGE, IFNOTPRESENT_IMAGEPULLPOLICY);
    }

    private V1Pod
        getActualAdminServerPodConfigForCustomLatestImageAndCustomImagePullPolicyAndDefaults()
            throws Exception {
      return getActualAdminServerPodConfig(
          getDomainCustomResourceForDefaults(
              CUSTOM_LATEST_IMAGE,
              IFNOTPRESENT_IMAGEPULLPOLICY), // custom latest image & custom image pull policy
          newEmptyPersistentVolumeClaimList());
    }

    @Test
    public void
        computedManagedServerPodConfigForCustomLatestImageAndCustomImagePullPolicyAndDefaults_isCorrect()
            throws Exception {
      assertThat(
          getActualManagedServerPodConfigForCustomLatestImageAndCustomImagePullPolicyAndDefaults(),
          yamlEqualTo(
              getDesiredManagedServerPodConfigForCustomLatestImageAndCustomImagePullPolicyAndDefaults()));
    }

    private V1Pod
        getDesiredManagedServerPodConfigForCustomLatestImageAndCustomImagePullPolicyAndDefaults() {
      return getDesiredManagedServerPodConfigForDefaults(
          CUSTOM_LATEST_IMAGE, IFNOTPRESENT_IMAGEPULLPOLICY);
    }

    private V1Pod
        getActualManagedServerPodConfigForCustomLatestImageAndCustomImagePullPolicyAndDefaults()
            throws Exception {
      return getActualManagedServerPodConfig(
          getDomainCustomResourceForDefaults(
              CUSTOM_LATEST_IMAGE,
              IFNOTPRESENT_IMAGEPULLPOLICY), // custom latest image & custom image pull policy
          newEmptyPersistentVolumeClaimList());
    }

    @Test
    public void computedAdminServerPodConfigForPersistentVolumeClaimAndDefaults_isCorrect()
        throws Exception {
      assertThat(
          getActualAdminServerPodConfigForPersistentVolumeClaimAndDefaults(),
          yamlEqualTo(getDesiredAdminServerPodConfigForPersistentVolumeClaimAndDefaults()));
    }

    private V1Pod getDesiredAdminServerPodConfigForPersistentVolumeClaimAndDefaults() {
      V1Pod pod =
          getDesiredAdminServerPodConfigForDefaults(DEFAULT_IMAGE, IFNOTPRESENT_IMAGEPULLPOLICY);
      setDesiredPersistentVolumeClaim(pod);
      return pod;
    }

    private V1Pod getActualAdminServerPodConfigForPersistentVolumeClaimAndDefaults()
        throws Exception {
      return getActualAdminServerPodConfig(
          getDomainCustomResourceForDefaults(null, null), // default image & default image pull policy
          newPersistentVolumeClaimListForPersistentVolumeClaim());
    }

    @Test
    public void computedManagedServerPodConfigForPersistentVolumeClaimAndDefaults_isCorrect()
        throws Exception {
      assertThat(
          getActualManagedServerPodConfigForPersistentVolumeClaimAndDefaults(),
          yamlEqualTo(getDesiredManagedServerPodConfigForPersistentVolumeClaimAndDefaults()));
    }

    private V1Pod getDesiredManagedServerPodConfigForPersistentVolumeClaimAndDefaults() {
      V1Pod pod =
          getDesiredManagedServerPodConfigForDefaults(DEFAULT_IMAGE, IFNOTPRESENT_IMAGEPULLPOLICY);
      setDesiredPersistentVolumeClaim(pod);
      return pod;
    }

    private V1Pod getActualManagedServerPodConfigForPersistentVolumeClaimAndDefaults()
        throws Exception {
      return getActualManagedServerPodConfig(
          getDomainCustomResourceForDefaults(null, null), // default image & default image pull policy
          newPersistentVolumeClaimListForPersistentVolumeClaim());
    }

    @Test
    public void computedAdminServerPodConfigForServerStartupAndDefaults_isCorrect() throws Exception {
      assertThat(
          getActualAdminServerPodConfigForServerStartupAndDefaults(),
          yamlEqualTo(getDesiredAdminServerPodConfigForServerStartupAndDefaults()));
    }

    private V1Pod getDesiredAdminServerPodConfigForServerStartupAndDefaults() {
      V1Pod pod =
          getDesiredAdminServerPodConfigForDefaults(DEFAULT_IMAGE, IFNOTPRESENT_IMAGEPULLPOLICY);
      // the custom env vars need to be added to the beginning of the list:
      pod.getSpec()
          .getContainers()
          .get(0)
          .getEnv()
          .add(0, newEnvVar().name(ADMIN_OPTION2_NAME).value(ADMIN_OPTION2_VALUE));
      pod.getSpec()
          .getContainers()
          .get(0)
          .getEnv()
          .add(0, newEnvVar().name(ADMIN_OPTION1_NAME).value(ADMIN_OPTION1_VALUE));
      return pod;
    }

    private V1Pod getActualAdminServerPodConfigForServerStartupAndDefaults() throws Exception {
      Domain domain =
          getDomainCustomResourceForDefaults(null, null); // default image & default image pull policy
      domain.getSpec().withServerStartup(newTestServersStartupList());
      return getActualAdminServerPodConfig(domain, newPersistentVolumeClaimList()); // no pvc
    }

    // don't test sending in a server startup list when creating a managed pod config since
    // PodHelper doesn't pay attention to the server startup list - intead, it uses the
    // packet's env vars (which we've already tested)

    private V1Pod getActualAdminServerPodConfig(Domain domain, V1PersistentVolumeClaimList claims)
        throws Exception {
      Packet packet = newPacket(domain, claims);
      packet.put(ProcessingConstants.SERVER_NAME, domain.getSpec().getAsName());
      packet.put(ProcessingConstants.PORT, domain.getSpec().getAsPort());
      return PodHelper.computeAdminPodConfig(newPodTuning(), INTERNAL_OPERATOR_CERT_FILE, packet);
    }
  */
  // TBD - test imagePullSecrets, startedServerState ADMIN
  /*
        .addImagePullSecretsItem(
            newLocalObjectReference().name(getInputs().getWeblogicImagePullSecretName()));

         serverConfig.
         withImagePullSecrets(List<V1LocalObjectReference> imagePullSecrets)

                     .withImagePullSecrets(
                newLocalObjectReferenceList().addElement(newLocalObjectReference().name("secret1")))
  */
  /*
    private V1Pod getActualManagedServerPodConfig(Domain domain, V1PersistentVolumeClaimList claims)
        throws Exception {
      Packet packet = newPacket(domain, claims);
      packet.put(
          ProcessingConstants.SERVER_SCAN,
          // no listen address, no network access points since PodHelper doesn't use them:
          new WlsServerConfig(
              MANAGED_SERVER_NAME, CLUSTER_NAME, MANAGED_SERVER_PORT, null, null, false, null, null));
      packet.put(
          ProcessingConstants.CLUSTER_SCAN,
          // don't attach WlsServerConfigs for the managed server to the WlsClusterConfig
          // since PodHelper doesn't use them:
          new WlsClusterConfig(CLUSTER_NAME));
      packet.put(
          ProcessingConstants.SERVER_CONFIG,
          new ServerConfig()
              .withServerName(MANAGED_SERVER_NAME)
              .withImage(DEFAULT_IMAGE)
              .withImagePullPolicy(IFNOTPRESENT_IMAGEPULLPOLICY)
              .withEnv(newEnvVarList()
                  .addElement(newEnvVar().name(MANAGED_OPTION1_NAME).value(MANAGED_OPTION1_VALUE))
                  .addElement(newEnvVar().name(MANAGED_OPTION2_NAME).value(MANAGED_OPTION2_VALUE))));
      return PodHelper.computeManagedPodConfig(newPodTuning(), packet);
    }
  */

  private static Packet newPacket(Domain domain, V1PersistentVolumeClaimList claims) {
    DomainPresenceInfo info = DomainPresenceInfoManager.getOrCreate(domain);
    info.setClaims(claims);
    Packet packet = new Packet();
    packet
        .getComponents()
        .put(ProcessingConstants.DOMAIN_COMPONENT_NAME, Component.createFor(info));
    return packet;
  }

  /*
    private List<ServerStartup> newTestServersStartupList() {
      return newServerStartupList()
          .addElement(
              newServerStartup()
                  .withDesiredState("RUNNING")
                  .withServerName(ADMIN_SERVER_NAME)
                  .withEnv(
                      newEnvVarList()
                          .addElement(newEnvVar().name(ADMIN_OPTION1_NAME).value(ADMIN_OPTION1_VALUE))
                          .addElement(
                              newEnvVar().name(ADMIN_OPTION2_NAME).value(ADMIN_OPTION2_VALUE))))
          .addElement(
              newServerStartup()
                  .withDesiredState("RUNNING")
                  .withServerName(MANAGED_SERVER_NAME)
                  .withEnv(
                      newEnvVarList()
                          .addElement(
                              newEnvVar().name(MANAGED_OPTION3_NAME).value(MANAGED_OPTION3_VALUE))
                          .addElement(
                              newEnvVar().name(MANAGED_OPTION4_NAME).value(MANAGED_OPTION4_VALUE))));
    }

    private Domain getDomainCustomResourceForDefaults(String image, String imagePullPolicy) {
      DomainSpec spec = newDomainSpec();
      spec.setDomainUID(DOMAIN_UID);
      spec.setDomainName(DOMAIN_NAME);
      spec.setAsName(ADMIN_SERVER_NAME);
      spec.setAdminSecret(newSecretReference().name(WEBLOGIC_CREDENTIALS_SECRET_NAME));
      spec.setAsPort(ADMIN_SERVER_PORT);
      if (image != null) {
        spec.setImage(image);
      }
      if (imagePullPolicy != null) {
        spec.setImagePullPolicy(imagePullPolicy);
      }
      Domain domain = new Domain();
      domain.setMetadata(newObjectMeta().namespace(NAMESPACE));
      domain.setSpec(spec);
      return domain;
    }

    private V1Pod getDesiredAdminServerPodConfigForDefaults(String image, String imagePullPolicy) {
      V1Pod pod =
          getDesiredBaseServerPodConfigForDefaults(
              image, imagePullPolicy, ADMIN_SERVER_NAME, ADMIN_SERVER_PORT);
      pod.getSpec().hostname(DOMAIN_UID + "-" + ADMIN_SERVER_NAME.toLowerCase());
      pod.getSpec()
          .getContainers()
          .get(0)
          .getEnv()
          .add(newEnvVar().name("INTERNAL_OPERATOR_CERT").value(INTERNAL_OPERATOR_CERT_FILE));
      return pod;
    }

    private V1Pod getDesiredManagedServerPodConfigForDefaults(String image, String imagePullPolicy) {
      V1Pod pod =
          getDesiredBaseServerPodConfigForDefaults(
              image, imagePullPolicy, MANAGED_SERVER_NAME, MANAGED_SERVER_PORT);
      pod.getSpec()
          .getContainers()
          .get(0)
          .getEnv()
          .add(0, newEnvVar().name(MANAGED_OPTION1_NAME).value(MANAGED_OPTION1_VALUE));
      pod.getSpec()
          .getContainers()
          .get(0)
          .getEnv()
          .add(1, newEnvVar().name(MANAGED_OPTION2_NAME).value(MANAGED_OPTION2_VALUE));
      pod.getMetadata().putLabelsItem(CLUSTERNAME_LABEL, CLUSTER_NAME);
      pod.getSpec()
          .getContainers()
          .get(0)
          .addCommandItem(ADMIN_SERVER_NAME)
          .addCommandItem(String.valueOf(ADMIN_SERVER_PORT));
      return pod;
    }

    private void setDesiredPersistentVolumeClaim(V1Pod pod) {
      pod.getSpec()
          .getVolumes()
          .add(
              0,
              newVolume() // needs to be first in the list
                  .name("weblogic-domain-storage-volume")
                  .persistentVolumeClaim(
                      newPersistentVolumeClaimVolumeSource()
                          .claimName(WEBLOGIC_DOMAIN_PERSISTENT_VOLUME_CLAIM_NAME)));
    }

    private V1Pod getDesiredBaseServerPodConfigForDefaults(
        String image, String imagePullPolicy, String serverName, int port) {
      String serverNameLC = serverName.toLowerCase();
      return newPod()
          .metadata(
              newObjectMeta()
                  .name(DOMAIN_UID + "-" + serverNameLC)
                  .namespace(NAMESPACE)
                  .putAnnotationsItem("prometheus.io/path", "/wls-exporter/metrics")
                  .putAnnotationsItem("prometheus.io/port", "" + port)
                  .putAnnotationsItem("prometheus.io/scrape", "true")
                  .putLabelsItem(RESOURCE_VERSION_LABEL, DOMAIN_V1)
                  .putLabelsItem(CREATEDBYOPERATOR_LABEL, "true")
                  .putLabelsItem(DOMAINNAME_LABEL, DOMAIN_NAME)
                  .putLabelsItem(DOMAINUID_LABEL, DOMAIN_UID)
                  .putLabelsItem(SERVERNAME_LABEL, serverName))
          .spec(
              newPodSpec()
                  .addContainersItem(
                      newContainer()
                          .name("weblogic-server")
                          .image(image)
                          .imagePullPolicy(imagePullPolicy)
                          .addCommandItem("/weblogic-operator/scripts/startServer.sh")
                          .addCommandItem(DOMAIN_UID)
                          .addCommandItem(serverName)
                          .addCommandItem(DOMAIN_NAME)
                          .lifecycle(
                              newLifecycle()
                                  .preStop(
                                      newHandler()
                                          .exec(
                                              newExecAction()
                                                  .addCommandItem(
                                                      "/weblogic-operator/scripts/stopServer.sh")
                                                  .addCommandItem(DOMAIN_UID)
                                                  .addCommandItem(serverName)
                                                  .addCommandItem(DOMAIN_NAME))))
                          .livenessProbe(
                              newProbe()
                                  .initialDelaySeconds(10)
                                  .periodSeconds(10)
                                  .timeoutSeconds(5)
                                  .failureThreshold(1)
                                  .exec(
                                      newExecAction()
                                          .addCommandItem(
                                              "/weblogic-operator/scripts/livenessProbe.sh")
                                          .addCommandItem(DOMAIN_NAME)
                                          .addCommandItem(serverName)))
                          .readinessProbe(
                              newProbe()
                                  .initialDelaySeconds(2)
                                  .periodSeconds(10)
                                  .timeoutSeconds(5)
                                  .failureThreshold(1)
                                  .exec(
                                      newExecAction()
                                          .addCommandItem(
                                              "/weblogic-operator/scripts/readinessProbe.sh")
                                          .addCommandItem(DOMAIN_NAME)
                                          .addCommandItem(serverName)))
                          .addPortsItem(newContainerPort().containerPort(port).protocol("TCP"))
                          .addEnvItem(newEnvVar().name("DOMAIN_NAME").value(DOMAIN_NAME))
                          .addEnvItem(
                              newEnvVar().name("DOMAIN_HOME").value("/shared/domain/" + DOMAIN_NAME))
                          .addEnvItem(newEnvVar().name("ADMIN_NAME").value(ADMIN_SERVER_NAME))
                          .addEnvItem(newEnvVar().name("ADMIN_PORT").value("" + ADMIN_SERVER_PORT))
                          .addEnvItem(newEnvVar().name("SERVER_NAME").value(serverName))
                          .addEnvItem(newEnvVar().name("ADMIN_USERNAME").value(null))
                          .addEnvItem(newEnvVar().name("ADMIN_PASSWORD").value(null))
                          .addVolumeMountsItem(
                              newVolumeMount() // TBD - why is the mount created if the volume doesn't
                                  // exist?
                                  .name("weblogic-domain-storage-volume")
                                  .mountPath("/shared"))
                          .addVolumeMountsItem(
                              newVolumeMount()
                                  .name("weblogic-credentials-volume")
                                  .mountPath("/weblogic-operator/secrets"))
                          .addVolumeMountsItem(
                              newVolumeMount()
                                  .name("weblogic-domain-cm-volume")
                                  .mountPath("/weblogic-operator/scripts")))
                  .addVolumesItem(
                      newVolume()
                          .name("weblogic-credentials-volume")
                          .secret(
                              newSecretVolumeSource().secretName(WEBLOGIC_CREDENTIALS_SECRET_NAME)))
                  .addVolumesItem(
                      newVolume()
                          .name("weblogic-domain-cm-volume")
                          .configMap(
                              newConfigMapVolumeSource()
                                  .name("weblogic-domain-cm")
                                  .defaultMode(365))));
    }
  */

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
  public void addHandlersAndProbes_addsCorrectResources() {
    ServerConfig serverConfig = (new ServerConfig()).withServerName(MANAGED_SERVER_NAME);
    V1Container actual = newContainer();
    PodHelper.addHandlersAndProbes(actual, DOMAIN_SPEC, serverConfig, newPodTuning());

    V1Container want = newContainer();
    addExpectedPreStopHandler(want, MANAGED_SERVER_NAME);
    addExpectedLivenessProbe(want, MANAGED_SERVER_NAME);
    addExpectedReadinessProbe(want, MANAGED_SERVER_NAME);

    assertThat(actual, equalTo(want));
  }

  @Test
  public void addPreStopHandler_addsCorrectHandler() {
    ServerConfig serverConfig = (new ServerConfig()).withServerName(MANAGED_SERVER_NAME);
    V1Container actual = newContainer();
    PodHelper.addPreStopHandler(actual, DOMAIN_SPEC, serverConfig);

    V1Container want = newContainer();
    addExpectedPreStopHandler(want, MANAGED_SERVER_NAME);

    assertThat(actual, equalTo(want));
  }

  private void addExpectedPreStopHandler(V1Container container, String serverName) {
    container.lifecycle(
        newLifecycle()
            .preStop(
                newHandler()
                    .exec(
                        newExecAction()
                            .addCommandItem("/weblogic-operator/scripts/stopServer.sh")
                            .addCommandItem(DOMAIN_UID)
                            .addCommandItem(serverName)
                            .addCommandItem(DOMAIN_NAME))));
  }

  @Test
  public void addLivenessProbe_addsCorrectProbe() {
    ServerConfig serverConfig = (new ServerConfig()).withServerName(MANAGED_SERVER_NAME);
    V1Container actual = newContainer();
    PodHelper.addLivenessProbe(actual, DOMAIN_SPEC, serverConfig, newPodTuning());

    V1Container want = newContainer();
    addExpectedLivenessProbe(want, MANAGED_SERVER_NAME);

    assertThat(actual, equalTo(want));
  }

  private void addExpectedLivenessProbe(V1Container container, String serverName) {
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
                    .addCommandItem(serverName)));
  }

  @Test
  public void addReadinessProbe_addsCorrectProbe() {
    ServerConfig serverConfig = (new ServerConfig()).withServerName(MANAGED_SERVER_NAME);
    V1Container actual = newContainer();
    PodHelper.addReadinessProbe(actual, DOMAIN_SPEC, serverConfig, newPodTuning());

    V1Container want = newContainer();
    addExpectedReadinessProbe(want, MANAGED_SERVER_NAME);

    assertThat(actual, equalTo(want));
  }

  private void addExpectedReadinessProbe(V1Container container, String serverName) {
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
                    .addCommandItem(MANAGED_SERVER_NAME)));
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
    addExpectedVolumes(want, true); // have a claim

    assertThat(actual, equalTo(want));
  }

  private void addExpectedVolumes(V1PodSpec podSpec, boolean haveClaim) {
    if (haveClaim) {
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
    V1Container actual = newContainer();
    PodHelper.addWeblogicServerEnv(
        actual,
        DOMAIN_SPEC,
        (new ServerConfig())
            .withServerName(MANAGED_SERVER_NAME)
            .withStartedServerState(ADMIN_STATE)
            .withEnv(newEnvVarList().addElement(ENV_VAR1)));

    V1Container want = newContainer().addEnvItem(ENV_VAR1).addEnvItem(ADMIN_STARTUP_MODE_ENV_VAR);
    addExpectedWeblogicEnvVars(want, MANAGED_SERVER_NAME);

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

    V1Pod want =
        newPod()
            .metadata(
                getExpectedServerPodMetadata(
                    CLUSTER_NAME, MANAGED_SERVER_NAME, MANAGED_SERVER_PORT, RESTARTED_LABEL1));

    assertThat(actual, equalTo(want));
  }

  private V1ObjectMeta getExpectedServerPodMetadata(
      String clusterName, String serverName, int port, String restartedLabel) {
    V1ObjectMeta metadata =
        newObjectMeta()
            .name(DOMAIN_UID + "-" + serverName.toLowerCase())
            .namespace(NAMESPACE)
            .putAnnotationsItem("prometheus.io/path", "/wls-exporter/metrics")
            .putAnnotationsItem("prometheus.io/port", "" + port)
            .putAnnotationsItem("prometheus.io/scrape", "true")
            .putLabelsItem(RESOURCE_VERSION_LABEL, DOMAIN_V1)
            .putLabelsItem(CREATEDBYOPERATOR_LABEL, "true")
            .putLabelsItem(DOMAINNAME_LABEL, DOMAIN_NAME)
            .putLabelsItem(DOMAINUID_LABEL, DOMAIN_UID)
            .putLabelsItem(SERVERNAME_LABEL, serverName);
    addExpectedClusterNameLabel(metadata, clusterName);
    addExpectedRestartedLabel(metadata, restartedLabel);
    return metadata;
  }

  @Test
  public void overrideContainerWeblogicEnvVars_addsEnvVars() {
    ServerConfig serverConfig = (new ServerConfig()).withServerName(MANAGED_SERVER_NAME);
    V1Container actual = newContainer();
    PodHelper.overrideContainerWeblogicEnvVars(actual, DOMAIN_SPEC, serverConfig);

    V1Container want = newContainer();
    addExpectedWeblogicEnvVars(want, MANAGED_SERVER_NAME);

    assertThat(actual, equalTo(want));
  }

  private void addExpectedWeblogicEnvVars(V1Container container, String serverName) {
    container
        .addEnvItem(newEnvVar("DOMAIN_NAME", DOMAIN_NAME))
        .addEnvItem(newEnvVar("DOMAIN_HOME", "/shared/domain/" + DOMAIN_NAME))
        .addEnvItem(newEnvVar("ADMIN_NAME", ADMIN_SERVER_NAME))
        .addEnvItem(newEnvVar("ADMIN_PORT", "" + ADMIN_SERVER_PORT))
        .addEnvItem(newEnvVar("SERVER_NAME", serverName))
        .addEnvItem(newEnvVar("ADMIN_USERNAME", null))
        .addEnvItem(newEnvVar("ADMIN_PASSWORD", null));
  }

  @Test
  public void addRestartedLabel_haveRestartedLabel_addsLabel() {
    ServerConfig serverConfig = (new ServerConfig()).withRestartedLabel(RESTARTED_LABEL1);
    V1ObjectMeta actual = newObjectMeta();
    PodHelper.addRestartedLabel(actual, serverConfig);

    V1ObjectMeta want = newObjectMeta();
    addExpectedRestartedLabel(want, RESTARTED_LABEL1);

    assertThat(actual, equalTo(want));
  }

  private void addExpectedRestartedLabel(V1ObjectMeta metadata, String restartedLabel) {
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
    addExpectedClusterNameLabel(want, CLUSTER_NAME);

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

  private void addExpectedClusterNameLabel(V1ObjectMeta metadata, String clusterName) {
    if (clusterName != null) {
      metadata.putLabelsItem(CLUSTERNAME_LABEL, clusterName);
    }
  }
}
