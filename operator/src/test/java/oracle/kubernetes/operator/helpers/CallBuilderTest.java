// Copyright 2018, Oracle Corporation and/or its affiliates.  All rights reserved.
// Licensed under the Universal Permissive License v 1.0 as shown at
// http://oss.oracle.com/licenses/upl.

package oracle.kubernetes.operator.helpers;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import oracle.kubernetes.operator.calls.CallResponse;
import oracle.kubernetes.operator.work.Engine;
import oracle.kubernetes.operator.work.Fiber.CompletionCallback;
import oracle.kubernetes.operator.work.NextAction;
import oracle.kubernetes.operator.work.Packet;
import oracle.kubernetes.operator.work.Step;
import oracle.kubernetes.weblogic.domain.v1.DomainList;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

// TODO fix this test so that it will not fail with 404 not found when run against a server
// that does not have the domain CRD defined.

@Ignore
public class CallBuilderTest {
  private static final String KEY = "domainlist";
  private static final String THROW = "throwables";

  private Engine engine = null;

  @Before
  public void setup() {
    engine = new Engine("CallBuilderTest");
  }

  @Test
  public void testListDomains() throws InterruptedException {
    Step stepline = new SetupStep(null);
    Packet p = new Packet();

    Semaphore signal = new Semaphore(0);
    List<Throwable> throwables = Collections.synchronizedList(new ArrayList<>());
    p.put(THROW, throwables);

    engine
        .createFiber()
        .start(
            stepline,
            p,
            new CompletionCallback() {
              @Override
              public void onCompletion(Packet packet) {
                signal.release();
              }

              @Override
              public void onThrowable(Packet packet, Throwable throwable) {
                throwables.add(throwable);
                signal.release();
              }
            });

    boolean result = signal.tryAcquire(20, TimeUnit.MINUTES);
    assertTrue(result);
    assertTrue(throwables.isEmpty());

    DomainList list = (DomainList) p.get(KEY);
    assertNotNull(list);
  }

  public static class SetupStep extends Step {

    SetupStep(Step next) {
      super(next);
    }

    @Override
    public NextAction apply(Packet packet) {
      String namespace = "default";

      CallBuilderFactory factory = new CallBuilderFactory();
      Step list =
          factory
              .create()
              .listDomainAsync(
                  namespace,
                  new ResponseStep<DomainList>(null) {

                    @SuppressWarnings("unchecked")
                    @Override
                    public NextAction onFailure(
                        Packet packet, CallResponse<DomainList> callResponse) {
                      if (callResponse.getE() != null) {
                        ((List<Throwable>) packet.get(THROW)).add(callResponse.getE());
                      }
                      return super.onFailure(packet, callResponse);
                    }

                    @Override
                    public NextAction onSuccess(
                        Packet packet, CallResponse<DomainList> callResponse) {
                      packet.put(KEY, callResponse.getResult());
                      return doNext(packet);
                    }
                  });

      return doNext(list, packet);
    }
  }
}
