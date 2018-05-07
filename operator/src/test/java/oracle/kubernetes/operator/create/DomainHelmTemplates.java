// Copyright 2018, Oracle Corporation and/or its affiliates.  All rights reserved.
// Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.

package oracle.kubernetes.operator.create;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages the generated helm chart templates for a domain
 */
public class DomainHelmTemplates {

  private static final String DOMAIN_CUSTOM_RESOURCE_YAML = "domain-custom-resource.yaml";
  private static final String CREATE_WEBLOGIC_DOMAIN_JOB_YAML = "create-weblogic-domain-job.yaml";
  private static final String WEBLOGIC_DOMAIN_PERSISTENT_VOLUME_YAML = "weblogic-domain-pv.yaml";
  private static final String WEBLOGIC_DOMAIN_PERSISTENT_VOLUME_CLAIM_YAML = "weblogic-domain-pvc.yaml";
  private static final String APACHE_YAML = "weblogic-domain-apache.yaml";
  private static final String APACHE_SECURITY_YAML = "weblogic-domain-apache-security.yaml";
  private static final String TRAEFIK_YAML = "weblogic-domain-traefik.yaml";
  private static final String TRAEFIK_SECURITY_YAML = "weblogic-domain-traefik-security.yaml";


  private Path userProjectsPath;
  private Path checkedInDomainTemplatesPath;

  public DomainHelmTemplates(Path userProjectsPath) {
    this.userProjectsPath = userProjectsPath;
    this.checkedInDomainTemplatesPath = Paths.get("../kubernetes/helm-charts/weblogic-domain/templates");
  }

  public Path userProjectsPath() { return userProjectsPath; }

  public Path getCreateWeblogicDomainJobYamlPath() {
    return getDomainTemplatesPath().resolve(CREATE_WEBLOGIC_DOMAIN_JOB_YAML);
  }

  public Path getDomainCustomResourceYamlPath() {
    return getDomainTemplatesPath().resolve(DOMAIN_CUSTOM_RESOURCE_YAML);
  }

  public Path getApacheYamlPath() {
    return getDomainTemplatesPath().resolve(APACHE_YAML);
  }

  public Path getApacheSecurityYamlPath() {
    return getDomainTemplatesPath().resolve(APACHE_SECURITY_YAML);
  }

  public Path getTraefikYamlPath() {
    return getDomainTemplatesPath().resolve(TRAEFIK_YAML);
  }

  public Path getTraefikSecurityYamlPath() {
    return getDomainTemplatesPath().resolve(TRAEFIK_SECURITY_YAML);
  }

  public Path getWeblogicDomainPersistentVolumeYamlPath() {
    return getDomainTemplatesPath().resolve(WEBLOGIC_DOMAIN_PERSISTENT_VOLUME_YAML);
  }

  public Path getWeblogicDomainPersistentVolumeClaimYamlPath() {
    return getDomainTemplatesPath().resolve(WEBLOGIC_DOMAIN_PERSISTENT_VOLUME_CLAIM_YAML);
  }

  public Path getDomainTemplatesPath() {
    return userProjectsPath().resolve("helm-charts/weblogic-domain/templates");
  }

  public Path getCheckedinDomainTemplatesPath() {
    return checkedInDomainTemplatesPath;
  }

  public List<Path> getExpectedContents(boolean includeDirectory) {
    List<Path> rtn = new ArrayList<>();
    rtn.add(getCreateWeblogicDomainJobYamlPath());
    rtn.add(getDomainCustomResourceYamlPath());
//    rtn.add(getApacheYamlPath());
//    rtn.add(getApacheSecurityYamlPath());
    rtn.add(getTraefikYamlPath());
    rtn.add(getTraefikSecurityYamlPath());
    rtn.add(getWeblogicDomainPersistentVolumeYamlPath());
    rtn.add(getWeblogicDomainPersistentVolumeClaimYamlPath());
    if (includeDirectory) {
      rtn.add(getDomainTemplatesPath());
    }
    return rtn;
  }
}
