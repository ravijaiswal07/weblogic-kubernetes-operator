// Copyright 2018, Oracle Corporation and/or its affiliates.  All rights reserved.
// Licensed under the Universal Permissive License v 1.0 as shown at
// http://oss.oracle.com/licenses/upl.

package oracle.kubernetes.operator.utils;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/** Manages helm chart templates for a weblogic operator */
public class OperatorHelmTemplates {

  private static final String ELASTICSEARCH_YAML = "elasticsearch.yaml";
  private static final String KIBANA_YAML = "kibana.yaml";
  private static final String WEBLOGIC_OPERATOR_YAML = "weblogic-operator.yaml";
  private static final String WEBLOGIC_OPERATOR_SECURITY_YAML = "weblogic-operator-security.yaml";

  private static final String HELM_TEMPLATES_PATH = "helm-charts/weblogic-operator/templates";

  private Path helmTemplatesPath;

  public OperatorHelmTemplates(Path helmTemplatesPath) {
    this.helmTemplatesPath = helmTemplatesPath;
  }

  /**
   * @return An OperatorHelmTemplates for helm chart templates that are under
   *     weblogic-kubernetes-operator/kubernetes/helm-charts directory
   */
  public static OperatorHelmTemplates getCheckedinTemplates() {
    return new OperatorHelmTemplates(Paths.get("../kubernetes", HELM_TEMPLATES_PATH));
  }

  public static OperatorHelmTemplates getOperatorHelmTemplates(Path userProjectsPath) {
    return new OperatorHelmTemplates(userProjectsPath.resolve(HELM_TEMPLATES_PATH));
  }

  public Path getElasticsearchYamlPath() {
    return helmTemplatesPath.resolve(ELASTICSEARCH_YAML);
  }

  public Path getKibanaYamlPath() {
    return helmTemplatesPath.resolve(KIBANA_YAML);
  }

  public Path getWeblogicOperatorYamlPath() {
    return helmTemplatesPath.resolve(WEBLOGIC_OPERATOR_YAML);
  }

  public Path getWeblogicOperatorSecurityYamlPath() {
    return helmTemplatesPath.resolve(WEBLOGIC_OPERATOR_SECURITY_YAML);
  }

  public Path getHelmTemplatesPath() {
    return helmTemplatesPath;
  }

  public List<Path> getExpectedContents(boolean includeDirectory) {
    List<Path> rtn = new ArrayList<>();
    rtn.add(getWeblogicOperatorYamlPath());
    rtn.add(getWeblogicOperatorSecurityYamlPath());
    rtn.add(getElasticsearchYamlPath());
    rtn.add(getKibanaYamlPath());
    if (includeDirectory) {
      rtn.add(helmTemplatesPath);
    }
    return rtn;
  }
}
