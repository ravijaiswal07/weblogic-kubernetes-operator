// Copyright 2018, Oracle Corporation and/or its affiliates.  All rights reserved.
// Licensed under the Universal Permissive License v 1.0 as shown at
// http://oss.oracle.com/licenses/upl.

package oracle.kubernetes.operator.helpers;

import static oracle.kubernetes.operator.VersionConstants.DOMAIN_V1;

import io.kubernetes.client.models.V1ConfigMap;
import io.kubernetes.client.models.V1ObjectMeta;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import oracle.kubernetes.operator.KubernetesConstants;
import oracle.kubernetes.operator.LabelConstants;
import oracle.kubernetes.operator.ProcessingConstants;
import oracle.kubernetes.operator.calls.CallResponse;
import oracle.kubernetes.operator.logging.LoggingFacade;
import oracle.kubernetes.operator.logging.LoggingFactory;
import oracle.kubernetes.operator.logging.MessageKeys;
import oracle.kubernetes.operator.steps.DefaultResponseStep;
import oracle.kubernetes.operator.work.NextAction;
import oracle.kubernetes.operator.work.Packet;
import oracle.kubernetes.operator.work.Step;

public class ConfigMapHelper {
  private static final LoggingFacade LOGGER = LoggingFactory.getLogger("Operator", "Operator");

  private static final String SCRIPTS = "scripts";
  private static final String SCRIPT_LOCATION = "/" + SCRIPTS;
  private static final ConfigMapHelperFacade FACADE = new ConfigMapHelperFacadeImpl();

  private ConfigMapHelper() {}

  /**
   * Factory for {@link Step} that creates config map containing scripts
   *
   * @param operatorNamespace the operator's namespace
   * @param domainNamespace the domain's namespace
   * @return Step for creating config map containing scripts
   */
  public static Step createScriptConfigMapStep(String operatorNamespace, String domainNamespace) {
    return new ScriptConfigMapStep(operatorNamespace, domainNamespace);
  }

  private static class ScriptConfigMapStep extends Step {
    private ConfigMapContext context;

    ScriptConfigMapStep(String operatorNamespace, String domainNamespace) {
      context = new ConfigMapContext(this, operatorNamespace, domainNamespace);
    }

    @Override
    public NextAction apply(Packet packet) {
      return doNext(context.readConfigMap(getNext()), packet);
    }
  }

  static Map<String, String> loadContents(Path rootDir) throws IOException {
    try (Stream<Path> walk = Files.walk(rootDir, 1)) {
      return walk.filter(path -> !Files.isDirectory(path))
          .collect(Collectors.toMap(ConfigMapHelper::asString, ConfigMapHelper::readContents));
    }
  }

  private static String asString(Path path) {
    return path.getFileName().toString();
  }

  private static String readContents(Path path) {
    try {
      return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    } catch (IOException io) {
      LOGGER.warning(MessageKeys.EXCEPTION, io);
      return "";
    }
  }

  interface ConfigMapHelperFacade {
    boolean containsAll(Map<String, String> actualData, Map<String, String> expectedData);
  }

  static class ConfigMapHelperFacadeImpl implements ConfigMapHelperFacade {

    @Override
    public boolean containsAll(Map<String, String> actualData, Map<String, String> expectedData) {
      return actualData.entrySet().containsAll(expectedData.entrySet());
    }
  }

  static class ConfigMapContext {
    private final V1ConfigMap model;
    private final Map<String, String> classpathScripts = loadScriptsFromClasspath();
    private final Step conflictStep;
    private final String operatorNamespace;
    private final String domainNamespace;

    ConfigMapContext(Step conflictStep, String operatorNamespace, String domainNamespace) {
      this.conflictStep = conflictStep;
      this.operatorNamespace = operatorNamespace;
      this.domainNamespace = domainNamespace;
      this.model = createModel(classpathScripts);
    }

    Step readConfigMap(Step next) {
      return new CallBuilder()
          .readConfigMapAsync(
              getModel().getMetadata().getName(), this.domainNamespace, new ReadResponseStep(next));
    }

    private class ReadResponseStep extends DefaultResponseStep<V1ConfigMap> {

      ReadResponseStep(Step nextStep) {
        super(nextStep);
      }

      @Override
      public NextAction onSuccess(Packet packet, CallResponse<V1ConfigMap> callResponse) {
        V1ConfigMap existingMap = callResponse.getResult();
        if (existingMap == null) {
          return doNext(createConfigMap(getNext()), packet);
        } else {
          if (isCompatibleMap(existingMap)) {
            logConfigMapExists();
            packet.put(ProcessingConstants.SCRIPT_CONFIG_MAP, existingMap);
            return doNext(packet);
          } else {
            return doNext(updateConfigMap(getNext(), existingMap), packet);
          }
        }
      }
    }

    Step createConfigMap(Step next) {
      return new CallBuilder()
          .createConfigMapAsync(domainNamespace, getModel(), new CreateResponseStep(next));
    }

    private class CreateResponseStep extends ResponseStep<V1ConfigMap> {

      CreateResponseStep(Step next) {
        super(next);
      }

      @Override
      public NextAction onFailure(Packet packet, CallResponse<V1ConfigMap> callResponse) {
        return onFailure(conflictStep, packet, callResponse);
      }

      @Override
      public NextAction onSuccess(Packet packet, CallResponse<V1ConfigMap> callResponse) {
        LOGGER.info(MessageKeys.CM_CREATED, domainNamespace);
        packet.put(ProcessingConstants.SCRIPT_CONFIG_MAP, callResponse.getResult());
        return doNext(packet);
      }
    }

    Step updateConfigMap(Step next, V1ConfigMap existingMap) {
      return new CallBuilder()
          .replaceConfigMapAsync(
              getModel().getMetadata().getName(),
              domainNamespace,
              createModel(getCombinedData(existingMap)),
              new ReplaceResponseStep(next));
    }

    Map<String, String> getCombinedData(V1ConfigMap existingConfigMap) {
      Map<String, String> updated = existingConfigMap.getData();
      updated.putAll(this.classpathScripts);
      return updated;
    }

    void logConfigMapExists() {
      LOGGER.fine(MessageKeys.CM_EXISTS, domainNamespace);
    }

    boolean isCompatibleMap(V1ConfigMap existingMap) {
      return VersionHelper.matchesResourceVersion(existingMap.getMetadata(), DOMAIN_V1)
          && FACADE.containsAll(existingMap.getData(), getModel().getData());
    }

    private class ReplaceResponseStep extends ResponseStep<V1ConfigMap> {

      ReplaceResponseStep(Step next) {
        super(next);
      }

      @Override
      public NextAction onFailure(Packet packet, CallResponse<V1ConfigMap> callResponse) {
        return onFailure(conflictStep, packet, callResponse);
      }

      @Override
      public NextAction onSuccess(Packet packet, CallResponse<V1ConfigMap> callResponse) {
        LOGGER.info(MessageKeys.CM_REPLACED, domainNamespace);
        packet.put(ProcessingConstants.SCRIPT_CONFIG_MAP, callResponse.getResult());
        return doNext(packet);
      }
    }

    V1ConfigMap getModel() {
      return model;
    }

    private V1ConfigMap createModel(Map<String, String> data) {
      return new V1ConfigMap()
          .apiVersion("v1")
          .kind("ConfigMap")
          .metadata(createMetadata())
          .data(data);
    }

    private V1ObjectMeta createMetadata() {
      return new V1ObjectMeta()
          .name(KubernetesConstants.DOMAIN_CONFIG_MAP_NAME)
          .namespace(domainNamespace)
          .putLabelsItem(LabelConstants.RESOURCE_VERSION_LABEL, DOMAIN_V1)
          .putLabelsItem(LabelConstants.OPERATORNAME_LABEL, operatorNamespace)
          .putLabelsItem(LabelConstants.CREATEDBYOPERATOR_LABEL, "true");
    }
  }

  private static Map<String, String> loadScriptsFromClasspath() {
    synchronized (ConfigMapHelper.class) {
      try {
        return loadContents(getScriptsPath());
      } catch (IOException e) {
        LOGGER.warning(MessageKeys.EXCEPTION, e);
        throw new RuntimeException(e);
      }
    }
  }

  private static Path getScriptsPath() {
    try {
      return toPath(ConfigMapHelper.class.getResource(SCRIPT_LOCATION).toURI());
    } catch (URISyntaxException | IOException e) {
      LOGGER.warning(MessageKeys.EXCEPTION, e);
      throw new RuntimeException(e);
    }
  }

  private static Path toPath(URI uri) throws IOException {
    return "jar".equals(uri.getScheme()) ? getPathInJar(uri) : Paths.get(uri);
  }

  private static Path getPathInJar(URI uri) throws IOException {
    try (FileSystem fileSystem = FileSystems.newFileSystem(uri, Collections.emptyMap())) {
      return fileSystem.getPath(SCRIPTS);
    }
  }
}
