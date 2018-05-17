// Copyright 2018, Oracle Corporation and/or its affiliates.  All rights reserved.
// Licensed under the Universal Permissive License v 1.0 as shown at
// http://oss.oracle.com/licenses/upl.

package oracle.kubernetes.operator.rest.resource;

import java.io.File;
import java.io.IOException;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import oracle.kubernetes.operator.logging.LoggingFacade;
import oracle.kubernetes.operator.logging.LoggingFactory;

/**
 * FilesResource is a jaxrs resource that implements the REST api for the /operator/{version}/files
 * path. It can be used to download files from the operator.
 */
public class FilesResource extends BaseResource {
  private static LoggingFacade LOGGER = LoggingFactory.getLogger("Operator", "Operator");

  /**
   * Construct a FilesResource.
   *
   * @param parent - the jaxrs resource that parents this resource.
   * @param pathSegment - the last path segment in the url to this resource.
   */
  public FilesResource(BaseResource parent, String pathSegment) {
    super(parent, pathSegment);
  }

  @GET
  @Produces(MediaType.APPLICATION_OCTET_STREAM)
  @Path("/{key}")
  public Response download(@PathParam("key") String key) throws IOException {
    File directory = new File("/operator/files");
    File file = new File(directory, key);
    if (!file.exists()) {
      return Response.status(Response.Status.NOT_FOUND).build();
    }
    return Response.ok(file, MediaType.APPLICATION_OCTET_STREAM)
        .header("Content-Disposition", "attachment; filename=\"" + file.getName() + "\"")
        .build();
  }
}
