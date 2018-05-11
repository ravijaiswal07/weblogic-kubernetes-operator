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
import oracle.kubernetes.operator.utils.DomainHelmTemplates;
import oracle.kubernetes.operator.utils.GeneratedHelmTemplates;
import oracle.kubernetes.operator.utils.UserProjects;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class GenerateDomainHelmTemplatesTest {

  private static UserProjects userProjects;
  private static DomainHelmTemplates generatedDomainHelmTemplates;
  private static DomainHelmTemplates expectedDomainHelmTemplates;

  @BeforeClass
  public static void setup() throws Exception {
    GeneratedHelmTemplates generatedHelmTemplates =
        GeneratedHelmTemplates.generateDomainTemplates();
    generatedDomainHelmTemplates = generatedHelmTemplates.getDomainHelmTemplates();
    userProjects = generatedHelmTemplates.getUserProjects();
    expectedDomainHelmTemplates = DomainHelmTemplates.getCheckedinTemplates();
  }

  @AfterClass
  public static void tearDown() throws Exception {
    if (userProjects != null) {
      userProjects.remove();
    }
  }

  @Test
  public void createDomainHelmTemplatesSucceedsAndGeneratesExpectedYamlFiles() throws Exception {
    assertThatOnlyTheExpectedGeneratedYamlFilesExist();
  }

  private void assertThatOnlyTheExpectedGeneratedYamlFilesExist() throws Exception {
    // Make sure the generated directory has the correct list of files
    DomainHelmTemplates domainHelmTemplates =
        DomainHelmTemplates.getDomainHelmTemplates(userProjects.getPath());
    List<Path> expectedFiles =
        domainHelmTemplates.getExpectedContents(true); // include the directory too
    List<Path> actualFiles = userProjects.getContents(domainHelmTemplates.getHelmTemplatesPath());
    assertThat(
        actualFiles, containsInAnyOrder(expectedFiles.toArray(new Path[expectedFiles.size()])));

    // Make sure that the yaml files are regular files
    for (Path path :
        domainHelmTemplates.getExpectedContents(false)) { // don't include the directory too
      assertThat("Expect that " + path + " is a regular file", Files.isRegularFile(path), is(true));
    }
  }

  @Test
  public void generatesCorrect_createWeblogicDomainJob() throws Exception {
    assertSameFileContents(
        generatedDomainHelmTemplates.getCreateWeblogicDomainJobYamlPath(),
        expectedDomainHelmTemplates.getCreateWeblogicDomainJobYamlPath());
  }

  @Test
  public void generatesCorrect_domainCustomResource() throws Exception {
    assertSameFileContents(
        generatedDomainHelmTemplates.getDomainCustomResourceYamlPath(),
        expectedDomainHelmTemplates.getDomainCustomResourceYamlPath());
  }

  @Test
  public void generatesCorrect_weblogicDomainPV() throws Exception {
    assertSameFileContents(
        generatedDomainHelmTemplates.getWeblogicDomainPersistentVolumeYamlPath(),
        expectedDomainHelmTemplates.getWeblogicDomainPersistentVolumeYamlPath());
  }

  @Test
  public void generatesCorrect_weblogicDomainPVClaim() throws Exception {
    assertSameFileContents(
        generatedDomainHelmTemplates.getWeblogicDomainPersistentVolumeClaimYamlPath(),
        expectedDomainHelmTemplates.getWeblogicDomainPersistentVolumeClaimYamlPath());
  }

  @Test
  public void generatesCorrect_traefik() throws Exception {
    assertSameFileContents(
        generatedDomainHelmTemplates.getTraefikYamlPath(),
        expectedDomainHelmTemplates.getTraefikYamlPath());
  }

  @Test
  public void generatesCorrect_traefikSecurity() throws Exception {
    assertSameFileContents(
        generatedDomainHelmTemplates.getTraefikSecurityYamlPath(),
        expectedDomainHelmTemplates.getTraefikSecurityYamlPath());
  }

  @Test
  public void generatesCorrect_apache() throws Exception {
    assertSameFileContents(
        generatedDomainHelmTemplates.getApacheYamlPath(),
        expectedDomainHelmTemplates.getApacheYamlPath());
  }

  @Test
  public void generatesCorrect_apacheSecurity() throws Exception {
    assertSameFileContents(
        generatedDomainHelmTemplates.getApacheSecurityYamlPath(),
        expectedDomainHelmTemplates.getApacheSecurityYamlPath());
  }

  @Test
  public void generatesCorrect_voyager() throws Exception {
    assertSameFileContents(
        generatedDomainHelmTemplates.getVoyagerYamlPath(),
        expectedDomainHelmTemplates.getVoyagerYamlPath());
  }
}
