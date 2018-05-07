// Copyright 2018, Oracle Corporation and/or its affiliates.  All rights reserved.
// Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.

package oracle.kubernetes.operator.create;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static oracle.kubernetes.operator.create.ExecResultMatcher.succeedsAndPrints;
import static oracle.kubernetes.operator.create.ExecGenerateHelmTemplates.execGenerateDomainTemplates;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;

/**
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
 */
public class GenerateHelmTemplatesTest {

  private UserProjects userProjects;
  private GeneratedHelmTemplates generatedHelmTemplates;

  @Before
  public void setup() throws Exception {
    userProjects = UserProjects.createUserProjectsDirectory();
    generatedHelmTemplates = GeneratedHelmTemplates.generateDomainTemplates();
  }

  @After
  public void tearDown() throws Exception {
    if (userProjects != null) {
      userProjects.remove();
    }
  }

  @Test
  public void createDomainHelmTemplatesSucceedsAndGeneratesExpectedYamlFiles() throws Exception {
    assertThat(execGenerateDomainTemplates(userProjects.getPath()), succeedsAndPrints("Completed"));
    assertThatOnlyTheExpectedGeneratedYamlFilesExist();
  }

  private void assertThatOnlyTheExpectedGeneratedYamlFilesExist() throws Exception {
    // Make sure the generated directory has the correct list of files
    DomainHelmTemplates domainHelmTemplates = new DomainHelmTemplates(userProjects.getPath());
    List<Path> expectedFiles = domainHelmTemplates.getExpectedContents(true); // include the directory too
    List<Path> actualFiles = userProjects.getContents(domainHelmTemplates.getDomainTemplatesPath());
    assertThat(
      actualFiles,
      containsInAnyOrder(expectedFiles.toArray(new Path[expectedFiles.size()])));

    // Make sure that the yaml files are regular files
    for (Path path : domainHelmTemplates.getExpectedContents(false)) { // don't include the directory too
      assertThat("Expect that " + path + " is a regular file", Files.isRegularFile(path), is(true));
    }
  }

  private void assertFileContents() {
    //TODO
    //assertEquals(FileUtils.readLines(expected), FileUtils.readLines(output))
  }
}
