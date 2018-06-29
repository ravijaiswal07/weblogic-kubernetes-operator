// Copyright 2018, Oracle Corporation and/or its affiliates.  All rights reserved.
// Licensed under the Universal Permissive License v 1.0 as shown at
// http://oss.oracle.com/licenses/upl.

package oracle.kubernetes.operator.utils;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/** Manages helm chart templates for a weblogic domain */
public class DomainHelmTemplates {

  private static final String DOMAIN_CUSTOM_RESOURCE_YAML = "domain-custom-resource.yaml";
  private static final String CREATE_WEBLOGIC_DOMAIN_JOB_YAML = "create-weblogic-domain-job.yaml";
  private static final String WEBLOGIC_DOMAIN_PERSISTENT_VOLUME_YAML = "weblogic-domain-pv.yaml";
  private static final String WEBLOGIC_DOMAIN_PERSISTENT_VOLUME_CLAIM_YAML =
      "weblogic-domain-pvc.yaml";
  private static final String APACHE_YAML = "weblogic-domain-apache.yaml";
  private static final String APACHE_SECURITY_YAML = "weblogic-domain-apache-security.yaml";
  private static final String TRAEFIK_YAML = "weblogic-domain-traefik.yaml";
  private static final String TRAEFIK_SECURITY_YAML = "weblogic-domain-traefik-security.yaml";
  private static final String VOYAGER_YAML = "weblogic-domain-voyager-ingress.yaml";
  private static final String VOYAGER_OPERATOR_YAML = "voyager-operator.yaml";
  private static final String VOYAGER_OPERATOR_SECURITY_YAML = "voyager-operator-security.yaml";

  private static final String HELM_TEMPLATES_PATH = "helm-charts/weblogic-domain/templates";

  private Path helmTemplatesPath;

  public DomainHelmTemplates(Path helmTemplatesPath) {
    this.helmTemplatesPath = helmTemplatesPath;
  }

  /**
   * @return A DomainHelmTemplates for helm chart templates that are under
   *     weblogic-kubernetes-operator/kubernetes/helm-charts directory
   */
  public static DomainHelmTemplates getCheckedinTemplates() {
    return new DomainHelmTemplates(Paths.get("../kubernetes", HELM_TEMPLATES_PATH));
  }

  public static DomainHelmTemplates getDomainHelmTemplates(Path userProjectsPath) {
    return new DomainHelmTemplates(userProjectsPath.resolve(HELM_TEMPLATES_PATH));
  }

  public Path getCreateWeblogicDomainJobYamlPath() {
    return helmTemplatesPath.resolve(CREATE_WEBLOGIC_DOMAIN_JOB_YAML);
  }

  public Path getDomainCustomResourceYamlPath() {
    return helmTemplatesPath.resolve(DOMAIN_CUSTOM_RESOURCE_YAML);
  }

  public Path getApacheYamlPath() {
    return helmTemplatesPath.resolve(APACHE_YAML);
  }

  public Path getApacheSecurityYamlPath() {
    return helmTemplatesPath.resolve(APACHE_SECURITY_YAML);
  }

  public Path getTraefikYamlPath() {
    return helmTemplatesPath.resolve(TRAEFIK_YAML);
  }

  public Path getTraefikSecurityYamlPath() {
    return helmTemplatesPath.resolve(TRAEFIK_SECURITY_YAML);
  }

  public Path getVoyagerYamlPath() {
    return helmTemplatesPath.resolve(VOYAGER_YAML);
  }

  public Path getVoyagerOperatorYamlPath() {
    return helmTemplatesPath.resolve(VOYAGER_OPERATOR_YAML);
  }

  public Path getVoyagerOperatorSecurityYamlPath() {
    return helmTemplatesPath.resolve(VOYAGER_OPERATOR_SECURITY_YAML);
  }

  public Path getWeblogicDomainPersistentVolumeYamlPath() {
    return helmTemplatesPath.resolve(WEBLOGIC_DOMAIN_PERSISTENT_VOLUME_YAML);
  }

  public Path getWeblogicDomainPersistentVolumeClaimYamlPath() {
    return helmTemplatesPath.resolve(WEBLOGIC_DOMAIN_PERSISTENT_VOLUME_CLAIM_YAML);
  }

  public Path getHelmTemplatesPath() {
    return helmTemplatesPath;
  }

  public List<Path> getExpectedContents(boolean includeDirectory) {
    List<Path> rtn = new ArrayList<>();
    rtn.add(getCreateWeblogicDomainJobYamlPath());
    rtn.add(getDomainCustomResourceYamlPath());
    rtn.add(getApacheYamlPath());
    rtn.add(getApacheSecurityYamlPath());
    rtn.add(getTraefikYamlPath());
    rtn.add(getTraefikSecurityYamlPath());
    rtn.add(getVoyagerYamlPath());
    rtn.add(getVoyagerOperatorYamlPath());
    rtn.add(getVoyagerOperatorSecurityYamlPath());
    rtn.add(getWeblogicDomainPersistentVolumeYamlPath());
    rtn.add(getWeblogicDomainPersistentVolumeClaimYamlPath());
    if (includeDirectory) {
      rtn.add(helmTemplatesPath);
    }
    return rtn;
  }
}
