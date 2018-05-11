// Copyright 2018, Oracle Corporation and/or its affiliates.  All rights reserved.
// Licensed under the Universal Permissive License v 1.0 as shown at
// http://oss.oracle.com/licenses/upl.

package oracle.kubernetes.operator.create;

import static oracle.kubernetes.operator.utils.FileUtils.assertSameFileContents;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import oracle.kubernetes.operator.utils.GeneratedHelmTemplates;
import oracle.kubernetes.operator.utils.OperatorHelmTemplates;
import oracle.kubernetes.operator.utils.UserProjects;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class GenerateOperatorHelmTemplatesTest {

  private static UserProjects userProjects;
  private static OperatorHelmTemplates generatedOperatorHelmTemplates;
  private static OperatorHelmTemplates expectedOperatorHelmTemplates;

  @BeforeClass
  public static void setup() throws Exception {
    GeneratedHelmTemplates generatedHelmTemplates =
        GeneratedHelmTemplates.generateOperatorTemplates();
    generatedOperatorHelmTemplates = generatedHelmTemplates.getOperatorHelmTemplates();
    userProjects = generatedHelmTemplates.getUserProjects();
    expectedOperatorHelmTemplates = OperatorHelmTemplates.getCheckedinTemplates();
  }

  @AfterClass
  public static void tearDown() throws Exception {
    if (userProjects != null) {
      userProjects.remove();
    }
  }

  @Test
  public void createOperatorHelmTemplatesSucceedsAndGeneratesExpectedYamlFiles() throws Exception {
    assertThatOnlyTheExpectedGeneratedYamlFilesExist();
  }

  private void assertThatOnlyTheExpectedGeneratedYamlFilesExist() throws Exception {
    // Make sure the generated directory has the correct list of files
    OperatorHelmTemplates operatorHelmTemplates =
        OperatorHelmTemplates.getOperatorHelmTemplates(userProjects.getPath());
    List<Path> expectedFiles =
        operatorHelmTemplates.getExpectedContents(true); // include the directory too
    List<Path> actualFiles = userProjects.getContents(operatorHelmTemplates.getHelmTemplatesPath());
    assertThat(
        actualFiles, containsInAnyOrder(expectedFiles.toArray(new Path[expectedFiles.size()])));

    // Make sure that the yaml files are regular files
    for (Path path :
        operatorHelmTemplates.getExpectedContents(false)) { // don't include the directory too
      assertThat("Expect that " + path + " is a regular file", Files.isRegularFile(path), is(true));
    }
  }

  @Test
  public void generatesCorrect_elasticsearch() throws Exception {
    assertSameFileContents(
        generatedOperatorHelmTemplates.getElasticsearchYamlPath(),
        expectedOperatorHelmTemplates.getElasticsearchYamlPath());
  }

  @Test
  public void generatesCorrect_kibana() throws Exception {
    assertSameFileContents(
        generatedOperatorHelmTemplates.getKibanaYamlPath(),
        expectedOperatorHelmTemplates.getKibanaYamlPath());
  }

  @Test
  public void generatesCorrect_weblogicOperator() throws Exception {
    assertSameFileContents(
        generatedOperatorHelmTemplates.getWeblogicOperatorYamlPath(),
        expectedOperatorHelmTemplates.getWeblogicOperatorYamlPath());
  }

  @Test
  public void generatesCorrect_weblogicOperatorSecurity() throws Exception {
    assertSameFileContents(
        generatedOperatorHelmTemplates.getWeblogicOperatorSecurityYamlPath(),
        expectedOperatorHelmTemplates.getWeblogicOperatorSecurityYamlPath());
  }
}
