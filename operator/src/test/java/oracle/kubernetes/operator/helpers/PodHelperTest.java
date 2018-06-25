// Copyright 2018, Oracle Corporation and/or its affiliates.  All rights reserved.
// Licensed under the Universal Permissive License v 1.0 as shown at
// http://oss.oracle.com/licenses/upl.

package oracle.kubernetes.operator.helpers;

import static com.meterware.simplestub.Stub.createStub;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.sameInstance;

import oracle.kubernetes.operator.PodAwaiterStepFactory;
import oracle.kubernetes.operator.work.Packet;
import org.junit.Test;

public class PodHelperTest {
  @Test
  public void afterAddingFactoryToPacket_canRetrieveIt() {
    Packet packet = new Packet();
    PodAwaiterStepFactory factory = createStub(PodAwaiterStepFactory.class);
    PodHelper.addToPacket(packet, factory);

    assertThat(PodHelper.getPodAwaiterStepFactory(packet), sameInstance(factory));
  }
}
