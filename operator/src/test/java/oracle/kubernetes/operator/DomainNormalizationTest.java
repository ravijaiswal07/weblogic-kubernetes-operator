// Copyright 2018, Oracle Corporation and/or its affiliates.  All rights reserved.
// Licensed under the Universal Permissive License v 1.0 as shown at
// http://oss.oracle.com/licenses/upl.

package oracle.kubernetes.operator;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;

import com.meterware.simplestub.Memento;
import io.kubernetes.client.models.V1EnvVar;
import java.util.ArrayList;
import java.util.List;
import oracle.kubernetes.TestUtils;
import oracle.kubernetes.weblogic.domain.v1.ClusterStartup;
import oracle.kubernetes.weblogic.domain.v1.DomainSpec;
import oracle.kubernetes.weblogic.domain.v1.ServerStartup;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class DomainNormalizationTest {

  private static final String LATEST_IMAGE = "store/oracle/weblogic:latest";
  private static final String IMAGE_PULL_POLICY = "Never";
  private static final String[] T3_CHANNELS = {"channel1", "channel2"};
  private static final String STARTUP_CONTROL = "ADMIN";
  private static final ServerStartup[] SERVER_STARTUPS = createServerStartups();
  private static final ClusterStartup[] CLUSTER_STARTUPS = createClusterStartups();
  private static final int REPLICAS = 5;
  private static final V1EnvVar ENV_VAR1 = new V1EnvVar().name("name1").value("value1");
  private static final V1EnvVar ENV_VAR2 = new V1EnvVar().name("name2").value("value2");
  private static final V1EnvVar ENV_VAR3 = new V1EnvVar().name("name3").value("value3");

  private final DomainSpec domainSpec = new DomainSpec();
  private List<Memento> mementos = new ArrayList<>();

  @Before
  public void setUp() throws Exception {
    mementos.add(TestUtils.silenceOperatorLogger());
  }

  @After
  public void tearDown() throws Exception {
    for (Memento memento : mementos) memento.revert();
  }

  @Test
  public void whenExportT3ChannelsHasNulls_normalizationSetsDefaultValues() throws Exception {
    domainSpec.setExportT3Channels(null);

    DomainPresenceControl.normalizeDomainSpec(domainSpec);

    assertThat(domainSpec.getExportT3Channels(), empty());
  }

  @Test
  public void whenDomainSpecHasDefinedValues_normalizationDoesNotChangeThem() throws Exception {
    domainSpec.setImage(LATEST_IMAGE);
    domainSpec.setImagePullPolicy(IMAGE_PULL_POLICY);
    domainSpec.setExportT3Channels(asList(T3_CHANNELS));
    domainSpec.setStartupControl(STARTUP_CONTROL);
    domainSpec.setServerStartup(asList(SERVER_STARTUPS));
    domainSpec.setClusterStartup(asList(CLUSTER_STARTUPS));
    domainSpec.setReplicas(REPLICAS);

    DomainPresenceControl.normalizeDomainSpec(domainSpec);

    assertThat(domainSpec.getImage(), equalTo(LATEST_IMAGE));
    assertThat(domainSpec.getImagePullPolicy(), equalTo(IMAGE_PULL_POLICY));
    assertThat(domainSpec.getExportT3Channels(), contains(T3_CHANNELS));
    assertThat(domainSpec.getStartupControl(), equalTo(STARTUP_CONTROL));
    assertThat(domainSpec.getServerStartup(), contains(SERVER_STARTUPS));
    assertThat(domainSpec.getClusterStartup(), contains(CLUSTER_STARTUPS));
    assertThat(domainSpec.getReplicas(), equalTo(REPLICAS));
  }

  private static ServerStartup[] createServerStartups() {
    return new ServerStartup[] {
      new ServerStartup().withDesiredState("STANDBY").withEnv(asList(ENV_VAR1, ENV_VAR2)),
      new ServerStartup().withDesiredState("RUNNING").withEnv(singletonList(ENV_VAR3))
    };
  }

  private static ClusterStartup[] createClusterStartups() {
    return new ClusterStartup[] {
      new ClusterStartup()
          .withDesiredState("ADMIN")
          .withEnv(asList(ENV_VAR1, ENV_VAR2, ENV_VAR3))
          .withReplicas(3)
    };
  }
}
