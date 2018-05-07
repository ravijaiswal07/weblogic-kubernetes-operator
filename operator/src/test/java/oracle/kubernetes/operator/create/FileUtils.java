// Copyright 2018, Oracle Corporation and/or its affiliates.  All rights reserved.
// Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
package oracle.kubernetes.operator.create;

import org.hamcrest.CoreMatchers;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeDiagnosingMatcher;

import java.util.Objects;

/**
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
 */
public class FileUtils {

  // Note: don't name it 'equalTo' since it conflicts with static importing
  // all the standard matchers, which would force callers to individually import
  // the standard matchers.
  public static FileMatcher fileEqualTo(Object expectedObject) {
    return new FileMatcher(expectedObject);
  }

  // Most k8s objects have an 'equals' implementation that works well across instances.
  // A few of the, e.g. V1 Secrets which prints out secrets as byte array addresses, don't.
  // For there kinds of objects, you can to convert them to yaml strings then comare those.
  // Anyway, it doesn't hurt to always just convert to yaml and compare the strings so that
  // we don't have to write type-dependent code.
  private static class FileMatcher extends TypeSafeDiagnosingMatcher<Object> {
    private Object expectedObject;

    private FileMatcher(Object expectedObject) {
      this.expectedObject = expectedObject;
    }

    @Override
    protected boolean matchesSafely(Object returnedObject, Description description) {
      String line;
      // TODO - implements this
      return true;
    }

    @Override
    public void describeTo(Description description) {
      description.appendText("\n").appendText(objectToYaml(expectedObject));
    }

    private String objectToYaml(Object object) {
      return YamlUtils.newYaml().dump(object);
    }
  }

}
