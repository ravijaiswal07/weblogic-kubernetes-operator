// Copyright 2017, 2018, Oracle Corporation and/or its affiliates.  All rights reserved.
// Licensed under the Universal Permissive License v 1.0 as shown at
// http://oss.oracle.com/licenses/upl.

package oracle.kubernetes.operator.steps;

import io.kubernetes.client.models.V1ObjectMeta;
import java.util.List;
import oracle.kubernetes.operator.helpers.DomainPresenceInfo;
import oracle.kubernetes.operator.work.NextAction;
import oracle.kubernetes.operator.work.Packet;
import oracle.kubernetes.operator.work.Step;
import oracle.kubernetes.weblogic.domain.v1.Domain;
import oracle.kubernetes.weblogic.domain.v1.DomainSpec;
import oracle.kubernetes.weblogic.domain.v1.ServerStartup;

public class BeforeAdminPodStep extends Step {

  public BeforeAdminPodStep(Step next) {
    super(next);
  }

  @Override
  public NextAction apply(Packet packet) {
    DomainPresenceInfo info = packet.getSPI(DomainPresenceInfo.class);

    Domain dom = info.getDomain();
    V1ObjectMeta meta = dom.getMetadata();
    DomainSpec spec = dom.getSpec();
    String namespace = meta.getNamespace();

    String podTemplateName = null;

    String asName = spec.getAsName();
    if (asName != null) {
      List<ServerStartup> lss = spec.getServerStartup();
      if (lss != null) {
        for (ServerStartup ss : lss) {
          if (asName.equals(ss.getServerName())) {
            podTemplateName = ss.getPodTemplate();
            break;
          }
        }
      }
    }

    if (podTemplateName != null) {
      return doNext(new ReadPodTemplateStep(podTemplateName, namespace, getNext()), packet);
    }
    return doNext(packet);
  }
}
