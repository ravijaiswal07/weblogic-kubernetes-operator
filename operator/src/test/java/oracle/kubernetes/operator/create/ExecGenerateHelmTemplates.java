// Copyright 2018, Oracle Corporation and/or its affiliates.  All rights reserved.
// Licensed under the Universal Permissive License v 1.0 as shown at
// http://oss.oracle.com/licenses/upl.

package oracle.kubernetes.operator.create;

import java.nio.file.Path;

/** Class for running create-weblogic-domain.sh */
public class ExecGenerateHelmTemplates {

  public static ExecResult execGenerateDomainTemplates(Path userProjectsPath) throws Exception {
    return ExecCreateDomain.execCreateDomain(" -m -o " + userProjectsPath.toString());
  }
}
