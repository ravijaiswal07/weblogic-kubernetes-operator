package oracle.kubernetes.operator;

import io.kubernetes.client.models.V1Pod;
import oracle.kubernetes.operator.work.Step;

public interface PodAwaiterStepFactory {
  /**
   * Waits until the Pod is Ready
   *
   * @param pod Pod to watch
   * @param next Next processing step once Pod is ready
   * @return Asynchronous step
   */
  Step waitForReady(V1Pod pod, Step next);
}
