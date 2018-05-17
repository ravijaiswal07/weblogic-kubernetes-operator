# Copyright 2017, 2018, Oracle Corporation and/or its affiliates.  All rights reserved.
# Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
#
# HOW TO BUILD THIS IMAGE
# -----------------------
# Run: 
#      $ docker build -t weblogic-kubernetes-operator:latest . 
#
# Pull base image
# From the Docker store
# -------------------------
FROM store/oracle/serverjre:8

# Maintainer
# ----------
MAINTAINER Ryan Eberhard <ryan.eberhard@oracle.com>

RUN mkdir /operator
RUN mkdir /operator/lib
RUN mkdir /operator/files
ENV PATH=$PATH:/operator

COPY src/scripts/* /operator/
COPY operator/target/weblogic-kubernetes-operator-1.0.jar /operator/weblogic-kubernetes-operator.jar
COPY operator/target/lib/*.jar /operator/lib/
COPY target/weblogic-deploy.zip /operator/files/weblogic-deploy.zip

HEALTHCHECK --interval=1m --timeout=10s \
  CMD /operator/livenessProbe.sh

WORKDIR /operator/

CMD ["/operator/operator.sh"]
