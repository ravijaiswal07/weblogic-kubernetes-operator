// Copyright 2017, 2018, Oracle Corporation and/or its affiliates.  All rights reserved.
// Licensed under the Universal Permissive License v 1.0 as shown at
// http://oss.oracle.com/licenses/upl.

package oracle.kubernetes.operator.helpers;

import static oracle.kubernetes.operator.LabelConstants.*;

import io.kubernetes.client.ApiException;
import io.kubernetes.client.models.V1ConfigMapVolumeSource;
import io.kubernetes.client.models.V1Container;
import io.kubernetes.client.models.V1ContainerPort;
import io.kubernetes.client.models.V1DeleteOptions;
import io.kubernetes.client.models.V1EnvVar;
import io.kubernetes.client.models.V1ExecAction;
import io.kubernetes.client.models.V1Handler;
import io.kubernetes.client.models.V1Lifecycle;
import io.kubernetes.client.models.V1ObjectMeta;
import io.kubernetes.client.models.V1PersistentVolumeClaimList;
import io.kubernetes.client.models.V1PersistentVolumeClaimVolumeSource;
import io.kubernetes.client.models.V1Pod;
import io.kubernetes.client.models.V1PodSpec;
import io.kubernetes.client.models.V1Probe;
import io.kubernetes.client.models.V1SecretVolumeSource;
import io.kubernetes.client.models.V1Status;
import io.kubernetes.client.models.V1Volume;
import io.kubernetes.client.models.V1VolumeMount;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import oracle.kubernetes.operator.DomainStatusUpdater;
import oracle.kubernetes.operator.KubernetesConstants;
import oracle.kubernetes.operator.LabelConstants;
import oracle.kubernetes.operator.PodWatcher;
import oracle.kubernetes.operator.ProcessingConstants;
import oracle.kubernetes.operator.TuningParameters;
import oracle.kubernetes.operator.TuningParameters.PodTuning;
import oracle.kubernetes.operator.VersionConstants;
import oracle.kubernetes.operator.WebLogicConstants;
import oracle.kubernetes.operator.calls.CallResponse;
import oracle.kubernetes.operator.logging.LoggingFacade;
import oracle.kubernetes.operator.logging.LoggingFactory;
import oracle.kubernetes.operator.logging.MessageKeys;
import oracle.kubernetes.operator.wlsconfig.WlsServerConfig;
import oracle.kubernetes.operator.work.Container;
import oracle.kubernetes.operator.work.ContainerResolver;
import oracle.kubernetes.operator.work.NextAction;
import oracle.kubernetes.operator.work.Packet;
import oracle.kubernetes.operator.work.Step;
import oracle.kubernetes.weblogic.domain.v1.Domain;
import oracle.kubernetes.weblogic.domain.v1.DomainSpec;

public class PodHelper {
  private static final String INTERNAL_OPERATOR_CERT_FILE = "internalOperatorCert";
  private static final String INTERNAL_OPERATOR_CERT_ENV = "INTERNAL_OPERATOR_CERT";
  private static final String JAVA_OPTIONS_ENV_NAME = "JAVA_OPTIONS";
  private static final String STARTUP_MODE_JAVA_OPTION = "-Dweblogic.management.startupMode=";
  private static final String ADMIN_STARTUP_MODE_JAVA_OPTION = STARTUP_MODE_JAVA_OPTION + "ADMIN";

  private static final LoggingFacade LOGGER = LoggingFactory.getLogger("Operator", "Operator");

  private PodHelper() {}

  /**
   * Factory for {@link Step} that creates admin server pod
   *
   * @param next Next processing step
   * @return Step for creating admin server pod
   */
  public static Step createAdminPodStep(Step next) {
    return new AdminPodStep(next);
  }

  private static class AdminPodStep extends Step {
    public AdminPodStep(Step next) {
      super(next);
    }

    @Override
    public NextAction apply(Packet packet) {
      Container c = ContainerResolver.getInstance().getContainer();
      CallBuilderFactory factory = c.getSPI(CallBuilderFactory.class);
      TuningParameters configMapHelper = c.getSPI(TuningParameters.class);

      // Compute the desired pod configuration for the admin server
      V1Pod adminPod =
          computeAdminPodConfig(
              getAdminServerConfig(packet),
              configMapHelper.getPodTuning(),
              configMapHelper.get(INTERNAL_OPERATOR_CERT_FILE),
              packet);

      // Verify if Kubernetes api server has a matching Pod
      // Create or replace, if necessary
      V1ObjectMeta metadata = adminPod.getMetadata();
      String podName = metadata.getName();
      String namespace = metadata.getNamespace();
      String weblogicDomainUID = metadata.getLabels().get(DOMAINUID_LABEL);
      String asName = metadata.getLabels().get(SERVERNAME_LABEL);

      DomainPresenceInfo info = packet.getSPI(DomainPresenceInfo.class);

      boolean isExplicitRestartThisServer =
          info.getExplicitRestartAdmin().get() || info.getExplicitRestartServers().contains(asName);

      ServerKubernetesObjects sko = ServerKubernetesObjectsManager.getOrCreate(info, asName);

      // First, verify existing Pod
      Step read =
          factory
              .create()
              .readPodAsync(
                  podName,
                  namespace,
                  new ResponseStep<V1Pod>(getNext()) {
                    @Override
                    public NextAction onFailure(
                        Packet packet,
                        ApiException e,
                        int statusCode,
                        Map<String, List<String>> responseHeaders) {
                      if (statusCode == CallBuilder.NOT_FOUND) {
                        return onSuccess(packet, null, statusCode, responseHeaders);
                      }
                      return super.onFailure(packet, e, statusCode, responseHeaders);
                    }

                    @Override
                    public NextAction onSuccess(Packet packet, CallResponse<V1Pod> callResponse) {
                      V1Pod result = callResponse.getResult();
                      if (result == null) {
                        info.getExplicitRestartAdmin().set(false);
                        info.getExplicitRestartServers().remove(asName);
                        Step create =
                            factory
                                .create()
                                .createPodAsync(
                                    namespace,
                                    adminPod,
                                    new ResponseStep<V1Pod>(getNext()) {
                                      @Override
                                      public NextAction onFailure(
                                          Packet packet,
                                          ApiException e,
                                          int statusCode,
                                          Map<String, List<String>> responseHeaders) {
                                        return super.onFailure(
                                            AdminPodStep.this,
                                            packet,
                                            e,
                                            statusCode,
                                            responseHeaders);
                                      }

                                      @Override
                                      public NextAction onSuccess(
                                          Packet packet,
                                          V1Pod result,
                                          int statusCode,
                                          Map<String, List<String>> responseHeaders) {

                                        LOGGER.info(
                                            MessageKeys.ADMIN_POD_CREATED,
                                            weblogicDomainUID,
                                            asName);
                                        if (result != null) {
                                          sko.getPod().set(result);
                                        }
                                        return doNext(packet);
                                      }
                                    });
                        return doNext(create, packet);
                      } else if (!isExplicitRestartThisServer
                          && validateCurrentPod(adminPod, result)) {
                        // existing Pod has correct spec
                        LOGGER.fine(MessageKeys.ADMIN_POD_EXISTS, weblogicDomainUID, asName);
                        sko.getPod().set(result);
                        return doNext(packet);
                      } else {
                        // we need to update the Pod
                        Step replace =
                            new CyclePodStep(
                                AdminPodStep.this,
                                podName,
                                namespace,
                                adminPod,
                                MessageKeys.ADMIN_POD_REPLACED,
                                weblogicDomainUID,
                                asName,
                                info,
                                sko,
                                getNext());
                        return doNext(replace, packet);
                      }
                    }
                  });

      return doNext(read, packet);
    }
  }

  private static class CyclePodStep extends Step {
    private final Step conflictStep;
    private final String podName;
    private final String namespace;
    private final V1Pod newPod;
    private final String messageKey;
    private final String weblogicDomainUID;
    private final String serverName;
    private final DomainPresenceInfo info;
    private final ServerKubernetesObjects sko;

    public CyclePodStep(
        Step conflictStep,
        String podName,
        String namespace,
        V1Pod newPod,
        String messageKey,
        String weblogicDomainUID,
        String serverName,
        DomainPresenceInfo info,
        ServerKubernetesObjects sko,
        Step next) {
      super(next);
      this.conflictStep = conflictStep;
      this.podName = podName;
      this.namespace = namespace;
      this.newPod = newPod;
      this.messageKey = messageKey;
      this.weblogicDomainUID = weblogicDomainUID;
      this.serverName = serverName;
      this.info = info;
      this.sko = sko;
    }

    @Override
    public NextAction apply(Packet packet) {
      V1DeleteOptions deleteOptions = new V1DeleteOptions();
      // Set to null so that watcher doesn't recreate pod with old spec
      sko.getPod().set(null);
      CallBuilderFactory factory =
          ContainerResolver.getInstance().getContainer().getSPI(CallBuilderFactory.class);
      Step delete =
          factory
              .create()
              .deletePodAsync(
                  podName,
                  namespace,
                  deleteOptions,
                  new ResponseStep<V1Status>(getNext()) {
                    @Override
                    public NextAction onFailure(
                        Packet packet,
                        ApiException e,
                        int statusCode,
                        Map<String, List<String>> responseHeaders) {
                      if (statusCode == CallBuilder.NOT_FOUND) {
                        return onSuccess(packet, null, statusCode, responseHeaders);
                      }
                      return super.onFailure(conflictStep, packet, e, statusCode, responseHeaders);
                    }

                    @Override
                    public NextAction onSuccess(
                        Packet packet, CallResponse<V1Status> callResponses) {
                      if (conflictStep instanceof AdminPodStep) {
                        info.getExplicitRestartAdmin().set(false);
                      }
                      info.getExplicitRestartServers().contains(serverName);
                      Step create =
                          factory
                              .create()
                              .createPodAsync(
                                  namespace,
                                  newPod,
                                  new ResponseStep<V1Pod>(getNext()) {
                                    @Override
                                    public NextAction onFailure(
                                        Packet packet,
                                        ApiException e,
                                        int statusCode,
                                        Map<String, List<String>> responseHeaders) {
                                      return super.onFailure(
                                          conflictStep, packet, e, statusCode, responseHeaders);
                                    }

                                    @Override
                                    public NextAction onSuccess(
                                        Packet packet,
                                        V1Pod result,
                                        int statusCode,
                                        Map<String, List<String>> responseHeaders) {

                                      LOGGER.info(messageKey, weblogicDomainUID, serverName);
                                      if (result != null) {
                                        sko.getPod().set(result);
                                      }

                                      PodWatcher pw = packet.getSPI(PodWatcher.class);
                                      return doNext(pw.waitForReady(result, getNext()), packet);
                                    }
                                  });
                      return doNext(create, packet);
                    }
                  });
      return doNext(delete, packet);
    }
  }

  /**
   * Factory for {@link Step} that creates managed server pod
   *
   * @param next Next processing step
   * @return Step for creating managed server pod
   */
  public static Step createManagedPodStep(Step next) {
    return new ManagedPodStep(next);
  }

  private static class ManagedPodStep extends Step {
    public ManagedPodStep(Step next) {
      super(next);
    }

    @Override
    public NextAction apply(Packet packet) {
      Container c = ContainerResolver.getInstance().getContainer();
      CallBuilderFactory factory = c.getSPI(CallBuilderFactory.class);
      TuningParameters configMapHelper = c.getSPI(TuningParameters.class);

      // Compute the desired pod configuration for the managed server
      V1Pod pod =
          computeManagedPodConfig(
              getManagedServerConfig(packet), configMapHelper.getPodTuning(), packet);

      // Verify if Kubernetes api server has a matching Pod
      // Create or replace, if necessary
      V1ObjectMeta metadata = pod.getMetadata();
      String podName = metadata.getName();
      String namespace = metadata.getNamespace();
      String weblogicDomainUID = metadata.getLabels().get(DOMAINUID_LABEL);
      String weblogicServerName = metadata.getLabels().get(SERVERNAME_LABEL);
      String weblogicClusterName = metadata.getLabels().get(CLUSTERNAME_LABEL);

      DomainPresenceInfo info = packet.getSPI(DomainPresenceInfo.class);

      boolean isExplicitRestartThisServer =
          info.getExplicitRestartServers().contains(weblogicServerName)
              || (weblogicClusterName != null
                  && info.getExplicitRestartClusters().contains(weblogicClusterName));

      ServerKubernetesObjects sko =
          ServerKubernetesObjectsManager.getOrCreate(info, weblogicServerName);

      // First, verify there existing Pod
      Step read =
          factory
              .create()
              .readPodAsync(
                  podName,
                  namespace,
                  new ResponseStep<V1Pod>(getNext()) {
                    @Override
                    public NextAction onFailure(
                        Packet packet,
                        ApiException e,
                        int statusCode,
                        Map<String, List<String>> responseHeaders) {
                      if (statusCode == CallBuilder.NOT_FOUND) {
                        return onSuccess(packet, null, statusCode, responseHeaders);
                      }
                      return super.onFailure(packet, e, statusCode, responseHeaders);
                    }

                    @Override
                    public NextAction onSuccess(
                        Packet packet,
                        V1Pod result,
                        int statusCode,
                        Map<String, List<String>> responseHeaders) {
                      if (result == null) {
                        info.getExplicitRestartServers().remove(weblogicServerName);
                        Step create =
                            factory
                                .create()
                                .createPodAsync(
                                    namespace,
                                    pod,
                                    new ResponseStep<V1Pod>(getNext()) {
                                      @Override
                                      public NextAction onFailure(
                                          Packet packet,
                                          ApiException e,
                                          int statusCode,
                                          Map<String, List<String>> responseHeaders) {
                                        return super.onFailure(
                                            ManagedPodStep.this,
                                            packet,
                                            e,
                                            statusCode,
                                            responseHeaders);
                                      }

                                      @Override
                                      public NextAction onSuccess(
                                          Packet packet,
                                          V1Pod result,
                                          int statusCode,
                                          Map<String, List<String>> responseHeaders) {

                                        LOGGER.info(
                                            MessageKeys.MANAGED_POD_CREATED,
                                            weblogicDomainUID,
                                            weblogicServerName);
                                        if (result != null) {
                                          sko.getPod().set(result);
                                        }
                                        return doNext(packet);
                                      }
                                    });
                        return doNext(
                            DomainStatusUpdater.createProgressingStep(
                                DomainStatusUpdater.MANAGED_SERVERS_STARTING_PROGRESS_REASON,
                                false,
                                create),
                            packet);
                      } else if (!isExplicitRestartThisServer && validateCurrentPod(pod, result)) {
                        // existing Pod has correct spec
                        LOGGER.fine(
                            MessageKeys.MANAGED_POD_EXISTS, weblogicDomainUID, weblogicServerName);
                        sko.getPod().set(result);
                        return doNext(packet);
                      } else {
                        // we need to update the Pod
                        // defer to Pod rolling step
                        Step replace =
                            new CyclePodStep(
                                ManagedPodStep.this,
                                podName,
                                namespace,
                                pod,
                                MessageKeys.MANAGED_POD_REPLACED,
                                weblogicDomainUID,
                                weblogicServerName,
                                info,
                                sko,
                                getNext());
                        synchronized (packet) {
                          @SuppressWarnings("unchecked")
                          Map<String, StepAndPacket> rolling =
                              (Map<String, StepAndPacket>)
                                  packet.get(ProcessingConstants.SERVERS_TO_ROLL);
                          if (rolling != null) {
                            rolling.put(
                                weblogicServerName,
                                new StepAndPacket(
                                    DomainStatusUpdater.createProgressingStep(
                                        DomainStatusUpdater
                                            .MANAGED_SERVERS_STARTING_PROGRESS_REASON,
                                        false,
                                        replace),
                                    packet.clone()));
                          }
                        }
                        return doEnd(packet);
                      }
                    }
                  });

      return doNext(read, packet);
    }
  }

  /**
   * Factory for {@link Step} that deletes server pod
   *
   * @param sko Server Kubernetes Objects
   * @param next Next processing step
   * @return Step for deleting server pod
   */
  public static Step deletePodStep(ServerKubernetesObjects sko, Step next) {
    return new DeletePodStep(sko, next);
  }

  private static class DeletePodStep extends Step {
    private final ServerKubernetesObjects sko;

    public DeletePodStep(ServerKubernetesObjects sko, Step next) {
      super(next);
      this.sko = sko;
    }

    @Override
    public NextAction apply(Packet packet) {
      DomainPresenceInfo info = packet.getSPI(DomainPresenceInfo.class);

      Domain dom = info.getDomain();
      V1ObjectMeta meta = dom.getMetadata();
      String namespace = meta.getNamespace();

      V1DeleteOptions deleteOptions = new V1DeleteOptions();
      // Set pod to null so that watcher doesn't try to recreate pod
      V1Pod oldPod = sko.getPod().getAndSet(null);
      if (oldPod != null) {
        CallBuilderFactory factory =
            ContainerResolver.getInstance().getContainer().getSPI(CallBuilderFactory.class);
        return doNext(
            factory
                .create()
                .deletePodAsync(
                    oldPod.getMetadata().getName(),
                    namespace,
                    deleteOptions,
                    new ResponseStep<V1Status>(getNext()) {
                      @Override
                      public NextAction onFailure(
                          Packet packet,
                          ApiException e,
                          int statusCode,
                          Map<String, List<String>> responseHeaders) {
                        if (statusCode == CallBuilder.NOT_FOUND) {
                          return onSuccess(packet, null, statusCode, responseHeaders);
                        }
                        return super.onFailure(packet, e, statusCode, responseHeaders);
                      }

                      @Override
                      public NextAction onSuccess(
                          Packet packet,
                          V1Status result,
                          int statusCode,
                          Map<String, List<String>> responseHeaders) {
                        return doNext(getNext(), packet);
                      }
                    }),
            packet);
      }
      return doNext(packet);
    }
  }

  protected static boolean validateCurrentPod(V1Pod build, V1Pod current) {
    // We want to detect changes that would require replacing an existing Pod
    // however, we've also found that Pod.equals(Pod) isn't right because k8s
    // returns fields, such as nodeName, even when export=true is specified.
    // Therefore, we'll just compare specific fields

    // PodHelper always creates the build pod and it always has metadata and containers
    // and names, so don't bother checking for nulls

    if (!validateCurrentHasMetadata(current)
        || !validateCurrentVersion(current)
        || !validateCurrentRestartedLabel(build, current)
        || !validateCurrentImagePullSecrets(build, current)
        || !validateCurrentContainers(build, current)) {
      return false;
    }
    return true;
  }

  protected static boolean validateCurrentHasMetadata(V1Pod current) {
    return current.getMetadata() != null;
  }

  protected static boolean validateCurrentVersion(V1Pod current) {
    return VersionHelper.matchesResourceVersion(
        current.getMetadata(), VersionConstants.DOMAIN_V1DOT1);
  }

  protected static boolean validateCurrentRestartedLabel(V1Pod build, V1Pod current) {
    String buildLabel = getRestartedLabel(build);
    if (buildLabel != null) {
      // the current pod is obsolete if it doesn't have the new restarted label
      return buildLabel.equals(getRestartedLabel(current));
    }
    // there is no new restarted label.
    // accept any restarted label (including null) on the current pod
    return true;
  }

  protected static String getRestartedLabel(V1Pod pod) {
    Map<String, String> labels = pod.getMetadata().getLabels();
    if (labels == null) {
      return null;
    }
    return labels.get(LabelConstants.RESTARTED_LABEL);
  }

  protected static boolean validateCurrentImagePullSecrets(V1Pod build, V1Pod current) {
    return compareUnordered(
        build.getSpec().getImagePullSecrets(), current.getSpec().getImagePullSecrets());
  }

  protected static boolean validateCurrentContainers(V1Pod build, V1Pod current) {
    List<V1Container> currentContainers = current.getSpec().getContainers();
    if (currentContainers == null) {
      return false;
    }
    List<V1Container> buildContainers = build.getSpec().getContainers();
    if (buildContainers.size() != currentContainers.size()) {
      return false;
    }
    for (V1Container buildContainer : build.getSpec().getContainers()) {
      V1Container currentContainer = findCurrentContainer(buildContainer, currentContainers);
      if (currentContainer == null) {
        return false;
      }
      if (!validateCurrentContainer(buildContainer, currentContainer)) {
        return false;
      }
    }
    return true;
  }

  protected static V1Container findCurrentContainer(
      V1Container buildContainer, List<V1Container> currentContainers) {
    String name = buildContainer.getName();
    for (V1Container currentContainer : currentContainers) {
      if (name.equals(currentContainer.getName())) {
        return currentContainer;
      }
    }
    return null;
  }

  protected static boolean validateCurrentContainer(V1Container build, V1Container current) {
    if (!build.getImage().equals(current.getImage())
        || !build.getImagePullPolicy().equals(current.getImagePullPolicy())) {
      return false;
    }
    if (!compareUnordered(build.getPorts(), current.getPorts())) {
      return false;
    }
    if (!compareUnordered(build.getEnv(), current.getEnv())) {
      return false;
    }
    if (!compareUnordered(build.getEnvFrom(), current.getEnvFrom())) {
      return false;
    }
    return true;
  }

  protected static <T> boolean compareUnordered(List<T> a, List<T> b) {
    if (a == b) {
      return true;
    } else if (a == null || b == null) {
      return false;
    }
    if (a.size() != b.size()) {
      return false;
    }

    List<T> bprime = new ArrayList<>(b);
    for (T at : a) {
      if (!bprime.remove(at)) {
        return false;
      }
    }
    return true;
  }

  protected static ServerConfig getAdminServerConfig(Packet packet) {
    // TBD - add some notes about clustered admin server limitations / bad practice ... && WDT ...
    return (ServerConfig) packet.get(ProcessingConstants.SERVER_CONFIG);

    // DomainSpec domainSpec = packet.getSPI(DomainPresenceInfo.class).getDomain().getSpec();
    // String adminServerName = domainSpec.getAsName();

    // return LifeCycleHelper.instance()
    // .getEffectiveNonClusteredServerConfig(
    // packet.getSPI(DomainPresenceInfo.class).getDomain(), adminServerName);
  }

  protected static ServerConfig getManagedServerConfig(Packet packet) {
    return (ServerConfig) packet.get(ProcessingConstants.SERVER_CONFIG);
  }

  protected static V1Pod computeAdminPodConfig(
      ServerConfig serverConfig, PodTuning tuning, String internalOperatorCert, Packet packet) {
    DomainSpec domainSpec = packet.getSPI(DomainPresenceInfo.class).getDomain().getSpec();

    V1Pod pod = computeBaseServerPodConfig(serverConfig, domainSpec.getAsPort(), tuning, packet);

    pod.getSpec().setHostname(pod.getMetadata().getName());

    V1Container container = pod.getSpec().getContainers().get(0);
    addEnvVar(container, INTERNAL_OPERATOR_CERT_ENV, internalOperatorCert);

    return pod;
  }

  protected static V1Pod computeManagedPodConfig(
      ServerConfig serverConfig, PodTuning tuning, Packet packet) {
    WlsServerConfig scan = (WlsServerConfig) packet.get(ProcessingConstants.SERVER_SCAN);
    V1Pod pod = computeBaseServerPodConfig(serverConfig, scan.getListenPort(), tuning, packet);

    V1Container container = pod.getSpec().getContainers().get(0);
    DomainSpec domainSpec = packet.getSPI(DomainPresenceInfo.class).getDomain().getSpec();
    container
        .addCommandItem(domainSpec.getAsName())
        .addCommandItem(String.valueOf(domainSpec.getAsPort()));

    return pod;
  }

  protected static V1Pod computeBaseServerPodConfig(
      ServerConfig serverConfig, int weblogicServerPort, PodTuning tuning, Packet packet) {
    // Extract info we're going to need below:
    DomainPresenceInfo info = packet.getSPI(DomainPresenceInfo.class);
    Domain domain = info.getDomain();
    DomainSpec domainSpec = domain.getSpec();

    V1PodSpec podSpec = new V1PodSpec();

    V1Container container = new V1Container();
    container.setName(KubernetesConstants.CONTAINER_NAME);

    setWeblogicServerImage(podSpec, container, serverConfig);
    addWeblogicServerPort(container, weblogicServerPort);

    addCommands(container, domainSpec, serverConfig, tuning);
    addVolumes(podSpec, domainSpec, info.getClaims());
    addVolumeMounts(container);
    addWeblogicServerEnv(container, domainSpec, serverConfig);

    V1Pod pod = new V1Pod();
    pod.setSpec(podSpec);
    addWeblogicServerPodMetadata(pod, domain, serverConfig, weblogicServerPort);

    List<V1Container> containers = new ArrayList<>();
    containers.add(container);
    podSpec.setContainers(containers);

    return pod;
  }

  protected static void setWeblogicServerImage(
      V1PodSpec podSpec, V1Container container, ServerConfig serverConfig) {
    container.image(serverConfig.getImage()).imagePullPolicy(serverConfig.getImagePullPolicy());
    podSpec.setImagePullSecrets(serverConfig.getImagePullSecrets());
  }

  protected static void addWeblogicServerPort(V1Container container, int weblogicServerPort) {
    V1ContainerPort containerPort =
        (new V1ContainerPort()).containerPort(weblogicServerPort).protocol("TCP");
    container.addPortsItem(containerPort);
  }

  protected static void addCommands(
      V1Container container, DomainSpec domainSpec, ServerConfig serverConfig, PodTuning tuning) {
    addServerStartCommand(container, domainSpec, serverConfig);
    addPreStopHandler(container, domainSpec, serverConfig);
    addLivenessProbe(container, domainSpec, serverConfig, tuning);
    addReadinessProbe(container, domainSpec, serverConfig, tuning);
  }

  protected static void addServerStartCommand(
      V1Container container, DomainSpec domainSpec, ServerConfig serverConfig) {
    container
        .addCommandItem("/weblogic-operator/scripts/startServer.sh")
        .addCommandItem(domainSpec.getDomainUID())
        .addCommandItem(serverConfig.getServerName())
        .addCommandItem(domainSpec.getDomainName());
  }

  protected static void addPreStopHandler(
      V1Container container, DomainSpec domainSpec, ServerConfig serverConfig) {
    // TBD - hook up to serverConfig shutdown related properties
    V1ExecAction lifecycleExecAction =
        (new V1ExecAction())
            .addCommandItem("/weblogic-operator/scripts/stopServer.sh")
            .addCommandItem(domainSpec.getDomainUID())
            .addCommandItem(serverConfig.getServerName())
            .addCommandItem(domainSpec.getDomainName());
    V1Handler preStopHandler = (new V1Handler()).exec(lifecycleExecAction);
    V1Lifecycle lifecycle = (new V1Lifecycle()).preStop(preStopHandler);
    container.setLifecycle(lifecycle);
  }

  protected static void addLivenessProbe(
      V1Container container, DomainSpec domainSpec, ServerConfig serverConfig, PodTuning tuning) {
    V1ExecAction livenessAction =
        (new V1ExecAction())
            .addCommandItem("/weblogic-operator/scripts/livenessProbe.sh")
            .addCommandItem(domainSpec.getDomainName())
            .addCommandItem(serverConfig.getServerName());
    V1Probe livenessProbe =
        (new V1Probe())
            .exec(livenessAction)
            .initialDelaySeconds(tuning.livenessProbeInitialDelaySeconds)
            .timeoutSeconds(tuning.livenessProbeTimeoutSeconds)
            .periodSeconds(tuning.livenessProbePeriodSeconds)
            .failureThreshold(1); // must be 1
    container.livenessProbe(livenessProbe);
  }

  protected static void addReadinessProbe(
      V1Container container, DomainSpec domainSpec, ServerConfig serverConfig, PodTuning tuning) {
    V1ExecAction readinessAction =
        (new V1ExecAction())
            .addCommandItem("/weblogic-operator/scripts/readinessProbe.sh")
            .addCommandItem(domainSpec.getDomainName())
            .addCommandItem(serverConfig.getServerName());
    V1Probe readinessProbe =
        (new V1Probe())
            .exec(readinessAction)
            .initialDelaySeconds(tuning.readinessProbeInitialDelaySeconds)
            .timeoutSeconds(tuning.readinessProbeTimeoutSeconds)
            .periodSeconds(tuning.readinessProbePeriodSeconds)
            .failureThreshold(1); // must be 1
    container.readinessProbe(readinessProbe);
  }

  protected static void addVolumes(
      V1PodSpec podSpec, DomainSpec domainSpec, V1PersistentVolumeClaimList claims) {
    addWeblogicDomainStorageVolume(podSpec, claims);
    addWeblogicCredentialsVolume(podSpec, domainSpec);
    addWeblogicDomainConfigMapVolume(podSpec);
  }

  protected static void addWeblogicDomainStorageVolume(
      V1PodSpec podSpec, V1PersistentVolumeClaimList claims) {
    if (!claims.getItems().isEmpty()) {
      V1PersistentVolumeClaimVolumeSource pvClaimSource =
          (new V1PersistentVolumeClaimVolumeSource())
              .claimName(claims.getItems().iterator().next().getMetadata().getName());
      V1Volume volume =
          (new V1Volume())
              .name("weblogic-domain-storage-volume")
              .persistentVolumeClaim(pvClaimSource);
      podSpec.addVolumesItem(volume);
    }
  }

  protected static void addWeblogicCredentialsVolume(V1PodSpec podSpec, DomainSpec domainSpec) {
    V1SecretVolumeSource secret =
        (new V1SecretVolumeSource()).secretName(domainSpec.getAdminSecret().getName());
    V1Volume volumeSecret = (new V1Volume()).name("weblogic-credentials-volume").secret(secret);
    podSpec.addVolumesItem(volumeSecret);
  }

  protected static void addWeblogicDomainConfigMapVolume(V1PodSpec podSpec) {
    V1ConfigMapVolumeSource cm =
        (new V1ConfigMapVolumeSource())
            .name(KubernetesConstants.DOMAIN_CONFIG_MAP_NAME)
            .defaultMode(0555); // read and execute
    V1Volume volumeDomainConfigMap =
        (new V1Volume()).name("weblogic-domain-cm-volume").configMap(cm);
    podSpec.addVolumesItem(volumeDomainConfigMap);
  }

  protected static void addVolumeMounts(V1Container container) {
    addWeblogicDomainStorageVolumeMount(container);
    addWeblogicCredentialsVolumeMount(container);
    addWeblogicDomainConfigMapVolumeMount(container);
  }

  protected static void addWeblogicCredentialsVolumeMount(V1Container container) {
    V1VolumeMount volumeMountSecret =
        (new V1VolumeMount())
            .name("weblogic-credentials-volume")
            .mountPath("/weblogic-operator/secrets")
            .readOnly(true);
    container.addVolumeMountsItem(volumeMountSecret);
  }

  protected static void addWeblogicDomainConfigMapVolumeMount(V1Container container) {
    V1VolumeMount volumeMountScripts =
        (new V1VolumeMount())
            .name("weblogic-domain-cm-volume")
            .mountPath("/weblogic-operator/scripts")
            .readOnly(true);
    container.addVolumeMountsItem(volumeMountScripts);
  }

  protected static void addWeblogicDomainStorageVolumeMount(V1Container container) {
    V1VolumeMount volumeMount =
        (new V1VolumeMount()).name("weblogic-domain-storage-volume").mountPath("/shared");
    container.addVolumeMountsItem(volumeMount);
  }

  protected static void addWeblogicServerEnv(
      V1Container container, DomainSpec domainSpec, ServerConfig serverConfig) {
    List<V1EnvVar> env = getWeblogicServerEnv(serverConfig);
    if (env != null) {
      for (V1EnvVar ev : env) {
        container.addEnvItem(ev);
      }
    }
    overrideContainerWeblogicEnvVars(container, domainSpec, serverConfig);
  }

  protected static List<V1EnvVar> getWeblogicServerEnv(ServerConfig serverConfig) {
    List<V1EnvVar> env = serverConfig.getEnv();
    if (WebLogicConstants.ADMIN_STATE.equals(serverConfig.getStartedServerState())) {
      env = createStartInAdminModeEnv(env);
    }
    return env;
  }

  protected static List<V1EnvVar> createStartInAdminModeEnv(List<V1EnvVar> env) {
    List<V1EnvVar> rtn = new ArrayList();

    boolean foundJavaOptions = false;
    if (env != null) {
      for (V1EnvVar e : env) {
        if (JAVA_OPTIONS_ENV_NAME.equals(e.getName())) {
          // Extend every existing JAVA_OPTIONS env var to say that the server
          // should be started in admin mode:
          if (e.getValueFrom() != null) {
            // We need to add to JAVA_OPTIONS, yet the env var is a reference
            // to a shared env var, so we'd change the env var there too.
            // We don't want to do this.
            throw new IllegalStateException();
          }
          foundJavaOptions = true;
          rtn.add(newAdminStartupModeJavaOptionsEnvVar(e.getValue()));
        } else {
          rtn.add(e);
        }
      }
    }

    if (!foundJavaOptions) {
      rtn.add(newAdminStartupModeJavaOptionsEnvVar(null));
    }

    return rtn;
  }

  protected static V1EnvVar newAdminStartupModeJavaOptionsEnvVar(String previousValue) {
    return (new V1EnvVar())
        .name(JAVA_OPTIONS_ENV_NAME)
        .value(adminStartupModeJavaOptions(previousValue));
  }

  protected static String adminStartupModeJavaOptions(String previousValue) {
    if (previousValue == null) {
      return ADMIN_STARTUP_MODE_JAVA_OPTION;
    }
    // TBD - we could check whether the previous value already sets startupMode=ADMIN.
    // For now, just prepend startupMode=ADMIN to the previousValue.
    // From a quick google search, it looks like the order in JAVA_OPTIONS
    // is not specified, though most JVMs use the rightmost value.
    // So, since we're prepending, the previous value would normally win.
    return ADMIN_STARTUP_MODE_JAVA_OPTION + " " + previousValue;
  }

  // Add an environment variable to a container
  private static void addEnvVar(V1Container container, String name, String value) {
    V1EnvVar envVar = new V1EnvVar();
    envVar.setName(name);
    envVar.setValue(value);
    container.addEnvItem(envVar);
  }

  protected static void addWeblogicServerPodMetadata(
      V1Pod pod, Domain domain, ServerConfig serverConfig, int weblogicServerPort) {
    V1ObjectMeta metadata = new V1ObjectMeta();

    metadata.setNamespace(domain.getMetadata().getNamespace());

    DomainSpec domainSpec = domain.getSpec();
    String weblogicDomainUID = domainSpec.getDomainUID();
    String weblogicServerName = serverConfig.getServerName();

    String podName = LegalNames.toPodName(weblogicDomainUID, weblogicServerName);
    metadata.setName(podName);

    AnnotationHelper.annotateForPrometheus(metadata, weblogicServerPort);

    metadata
        .putLabelsItem(RESOURCE_VERSION_LABEL, VersionConstants.DOMAIN_V1DOT1)
        .putLabelsItem(DOMAINUID_LABEL, weblogicDomainUID)
        .putLabelsItem(DOMAINNAME_LABEL, domainSpec.getDomainName())
        .putLabelsItem(SERVERNAME_LABEL, weblogicServerName)
        .putLabelsItem(CREATEDBYOPERATOR_LABEL, "true");
    addRestartedLabel(metadata, serverConfig);
    addClusterNameLabel(metadata, serverConfig);

    pod.setMetadata(metadata);
  }

  // Override the weblogic domain and admin server related environment variables that
  // come for free with the WLS docker container with the correct values.
  protected static void overrideContainerWeblogicEnvVars(
      V1Container container, DomainSpec domainSpec, ServerConfig serverConfig) {
    // Override the domain name, domain directory, admin server name and admin server port.
    String weblogicDomainName = domainSpec.getDomainName();
    addEnvVar(container, "DOMAIN_NAME", weblogicDomainName);
    addEnvVar(container, "DOMAIN_HOME", "/shared/domain/" + weblogicDomainName);
    addEnvVar(container, "ADMIN_NAME", domainSpec.getAsName());
    addEnvVar(container, "ADMIN_PORT", domainSpec.getAsPort().toString());
    addEnvVar(container, "SERVER_NAME", serverConfig.getServerName());
    // Hide the admin account's user name and password.
    // Note: need to use null v.s. "" since if you upload a "" to kubectl then download it,
    // it comes back as a null and V1EnvVar.equals returns false even though it's supposed to
    // be the same value.
    // Regardless, the pod ends up with an empty string as the value (v.s. thinking that
    // the environment variable hasn't been set), so it honors the value (instead of using
    // the default, e.g. 'weblogic' for the user name).
    addEnvVar(container, "ADMIN_USERNAME", null);
    addEnvVar(container, "ADMIN_PASSWORD", null);
  }

  protected static void addRestartedLabel(V1ObjectMeta metadata, ServerConfig serverConfig) {
    String restartedLabel = serverConfig.getRestartedLabel();
    if (restartedLabel != null) {
      metadata.putLabelsItem(RESTARTED_LABEL, restartedLabel);
    }
  }

  protected static void addClusterNameLabel(V1ObjectMeta metadata, ServerConfig serverConfig) {
    if (serverConfig instanceof ClusteredServerConfig) {
      String weblogicClusterName = ((ClusteredServerConfig) serverConfig).getClusterName();
      if (weblogicClusterName == null) {
        throw new AssertionError("Null weblogicClusterName: " + serverConfig);
      }
      metadata.putLabelsItem(CLUSTERNAME_LABEL, weblogicClusterName);
    }
  }
}
