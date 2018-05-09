// Copyright 2017, 2018, Oracle Corporation and/or its affiliates.  All rights reserved.
// Licensed under the Universal Permissive License v 1.0 as shown at
// http://oss.oracle.com/licenses/upl.

package oracle.kubernetes.operator.steps;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import oracle.kubernetes.operator.ProcessingConstants;
import oracle.kubernetes.operator.helpers.DomainPresenceInfo;
import oracle.kubernetes.operator.work.NextAction;
import oracle.kubernetes.operator.work.Packet;
import oracle.kubernetes.operator.work.Step;
import oracle.kubernetes.weblogic.domain.v1.Domain;

public class ManagedServerUpBeforeStep extends Step {
  private final Collection<StepAndPacket> startDetailsAfter;

  public ManagedServerUpBeforeStep(Collection<StepAndPacket> startDetailsAfter, Step next) {
    super(next);
    this.startDetailsAfter = startDetailsAfter;
  }

  @Override
  public NextAction apply(Packet packet) {
    Set<String> podTemplateNames = new HashSet<>();
    for (StepAndPacket sap : startDetailsAfter) {
      String name = (String) sap.packet.get(ProcessingConstants.POD_TEMPLATE_NAME);
      if (name != null) {
        podTemplateNames.add(name);
      }
    }

    Step after = new DoForkJoinStep(startDetailsAfter, next);

    if (podTemplateNames.isEmpty()) {
      return doNext(after, packet);
    }

    DomainPresenceInfo info = packet.getSPI(DomainPresenceInfo.class);
    Domain dom = info.getDomain();
    String namespace = dom.getMetadata().getNamespace();
    Collection<StepAndPacket> startDetails = new ArrayList<>();

    for (String name : podTemplateNames) {
      startDetails.add(new StepAndPacket(new ReadPodTemplateStep(name, namespace, null), packet));
    }
    return doForkJoin(after, packet, startDetails);
  }

  private static class DoForkJoinStep extends Step {
    private final Collection<StepAndPacket> startDetailsAfter;

    public DoForkJoinStep(Collection<StepAndPacket> startDetailsAfter, Step next) {
      super(next);
      this.startDetailsAfter = startDetailsAfter;
    }

    @Override
    public NextAction apply(Packet packet) {
      return doForkJoin(next, packet, startDetailsAfter);
    }
  }
}
