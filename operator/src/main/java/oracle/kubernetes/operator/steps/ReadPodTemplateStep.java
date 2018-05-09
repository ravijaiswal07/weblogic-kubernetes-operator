// Copyright 2017, 2018, Oracle Corporation and/or its affiliates.  All rights reserved.
// Licensed under the Universal Permissive License v 1.0 as shown at
// http://oss.oracle.com/licenses/upl.

package oracle.kubernetes.operator.steps;

import io.kubernetes.client.models.V1PodTemplate;
import java.util.List;
import java.util.Map;
import oracle.kubernetes.operator.ProcessingConstants;
import oracle.kubernetes.operator.helpers.CallBuilderFactory;
import oracle.kubernetes.operator.helpers.ResponseStep;
import oracle.kubernetes.operator.work.ContainerResolver;
import oracle.kubernetes.operator.work.NextAction;
import oracle.kubernetes.operator.work.Packet;
import oracle.kubernetes.operator.work.Step;

public class ReadPodTemplateStep extends Step {
  private final String podTemplateName;
  private final String namespace;

  public ReadPodTemplateStep(String podTemplateName, String namespace, Step next) {
    super(next);
    this.podTemplateName = podTemplateName;
    this.namespace = namespace;
  }

  @Override
  public NextAction apply(Packet packet) {
    CallBuilderFactory factory =
        ContainerResolver.getInstance().getContainer().getSPI(CallBuilderFactory.class);
    return doNext(
        factory
            .create()
            .readPodTemplateAsync(
                podTemplateName,
                namespace,
                new ResponseStep<V1PodTemplate>(next) {

                  @Override
                  public NextAction onSuccess(
                      Packet packet,
                      V1PodTemplate result,
                      int statusCode,
                      Map<String, List<String>> responseHeaders) {
                    @SuppressWarnings("unchecked")
                    Map<String, V1PodTemplate> podTemplates =
                        (Map<String, V1PodTemplate>) packet.get(ProcessingConstants.POD_TEMPLATES);
                    podTemplates.put(podTemplateName, result);
                    return doNext(packet);
                  }
                }),
        packet);
  }
}
