// Copyright 2018, Oracle Corporation and/or its affiliates.  All rights reserved.
// Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.

package oracle.kubernetes.operator.create;

import static oracle.kubernetes.operator.create.ExecResultMatcher.succeedsAndPrints;
import static oracle.kubernetes.operator.create.UserProjects.createUserProjectsDirectory;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Generates the helm chart templates yaml files
 * Creates and managed the user projects directory that the files are stored in.
 */
public class GeneratedHelmTemplates {

  private UserProjects userProjects;
  private DomainHelmTemplates domainHelmTemplates;

  public static GeneratedHelmTemplates generateDomainTemplates() throws Exception {
    return new GeneratedHelmTemplates();
  }

  private GeneratedHelmTemplates() throws Exception {
    userProjects = createUserProjectsDirectory();
    boolean ok = false;
    try {
      domainHelmTemplates = new DomainHelmTemplates(userProjects.getPath());
      assertThat(ExecGenerateHelmTemplates.execGenerateDomainTemplates(userProjects.getPath()), succeedsAndPrints("Completed"));
    } finally {
      if (!ok) {
        remove();
      }
    }
  }

  public DomainHelmTemplates getDomainHelmTemplates() { return domainHelmTemplates; }

  public void remove() throws Exception {
    userProjects.remove();
  }
}
