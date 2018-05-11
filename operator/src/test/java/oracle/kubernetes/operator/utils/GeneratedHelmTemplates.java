// Copyright 2018, Oracle Corporation and/or its affiliates.  All rights reserved.
// Licensed under the Universal Permissive License v 1.0 as shown at
// http://oss.oracle.com/licenses/upl.

package oracle.kubernetes.operator.utils;

import static oracle.kubernetes.operator.utils.ExecResultMatcher.succeedsAndPrints;
import static oracle.kubernetes.operator.utils.UserProjects.createUserProjectsDirectory;
import static org.hamcrest.MatcherAssert.assertThat;

import java.nio.file.Path;

/** Generates and manages the helm chart templates yaml files */
public class GeneratedHelmTemplates {

  private UserProjects userProjects;
  private DomainHelmTemplates domainHelmTemplates;
  private OperatorHelmTemplates operatorHelmTemplates;

  public static GeneratedHelmTemplates generateDomainTemplates() throws Exception {
    return new GeneratedHelmTemplates(false, true);
  }

  public static GeneratedHelmTemplates generateOperatorTemplates() throws Exception {
    return new GeneratedHelmTemplates(true, false);
  }

  private GeneratedHelmTemplates(boolean operator, boolean domain) throws Exception {
    userProjects = createUserProjectsDirectory();
    boolean ok = false;
    try {
      if (domain) {
        domainHelmTemplates = DomainHelmTemplates.getDomainHelmTemplates(userProjects.getPath());
        assertThat(
            execGenerateDomainTemplates(userProjects.getPath()), succeedsAndPrints("Completed"));
        ok = true;
      }
      if (operator) {
        operatorHelmTemplates =
            OperatorHelmTemplates.getOperatorHelmTemplates(userProjects.getPath());
        assertThat(
            execGenerateOperatorTemplates(userProjects.getPath()), succeedsAndPrints("Completed"));
        ok = true;
      }
    } finally {
      if (!ok) {
        remove();
      }
    }
  }

  public static ExecResult execGenerateDomainTemplates(Path userProjectsPath) throws Exception {
    return ExecCreateDomain.execCreateDomain(" -m -o " + userProjectsPath.toString());
  }

  public static ExecResult execGenerateOperatorTemplates(Path userProjectsPath) throws Exception {
    return ExecCreateOperator.execCreateOperator(" -m -o " + userProjectsPath.toString());
  }

  public DomainHelmTemplates getDomainHelmTemplates() {
    return domainHelmTemplates;
  }

  public OperatorHelmTemplates getOperatorHelmTemplates() {
    return operatorHelmTemplates;
  }

  public UserProjects getUserProjects() {
    return userProjects;
  }

  public void remove() throws Exception {
    userProjects.remove();
  }
}
