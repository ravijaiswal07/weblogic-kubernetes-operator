// Copyright 2018, Oracle Corporation and/or its affiliates.  All rights reserved.
// Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.

package oracle.kubernetes.operator.helpers;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import io.kubernetes.client.custom.IntOrString;
import static oracle.kubernetes.operator.KubernetesConstants.*;
import oracle.kubernetes.weblogic.domain.v1.DomainSpec;
import oracle.kubernetes.weblogic.domain.v1.Cluster;
import oracle.kubernetes.weblogic.domain.v1.ClusterParams;
import oracle.kubernetes.weblogic.domain.v1.ClusteredServer;
import oracle.kubernetes.weblogic.domain.v1.NonClusteredServer;
import oracle.kubernetes.weblogic.domain.v1.Server;
import org.apache.commons.lang3.ArrayUtils;

/**
 * This helper class uses the domain spec that the customer configured to
 * calculate the effective configuration for the servers and clusters in the domain.
 */
public class LifeCycleHelper {

  protected LifeCycleHelper() {}

  private static final LifeCycleHelper INSTANCE = new LifeCycleHelper();

  private static final String SERVER_START_POLICY_ALWAYS = "ALWAYS";
  private static final String SERVER_START_POLICY_NEVER = "ALWAYS";

  public static final String STARTED_SERVER_STATE_RUNNING = "RUNNING";
  public static final String STARTED_SERVER_STATE_ADMIN = "ADMIN";

  public static final String SHUTDOWN_POLICY_FORCED_SHUTDOWN = "FORCED_SHUTDOWN";
  public static final String SHUTDOWN_POLICY_GRACEFUL_SHUTDOWN = "GRACEFUL_SHUTDOWN";

  public static final String NON_CLUSTERED_SERVER_START_POLICY_ALWAYS = SERVER_START_POLICY_ALWAYS;
  public static final String NON_CLUSTERED_SERVER_START_POLICY_NEVER = SERVER_START_POLICY_NEVER;

  public static final String CLUSTERED_SERVER_START_POLICY_ALWAYS = SERVER_START_POLICY_ALWAYS;
  public static final String CLUSTERED_SERVER_START_POLICY_NEVER = SERVER_START_POLICY_NEVER;
  public static final String CLUSTERED_SERVER_START_POLICY_IF_NEEDED = "IF_NEEDED";

  protected static Server SERVER_DEFAULTS =
    (new Server())
      .withStartedServerState(STARTED_SERVER_STATE_RUNNING)
      .withRestartedLabel(null)
      .withNodePort(null)
      .withEnv(null)
      .withImage(DEFAULT_IMAGE)
      .withImagePullPolicy(IFNOTPRESENT_IMAGEPULLPOLICY)
      .withImagePullSecrets(null) // TBD - should it be an empty list?
      .withShutdownPolicy(SHUTDOWN_POLICY_FORCED_SHUTDOWN) // TBD - is this correct?
      .withGracefulShutdownTimeout(new Integer(0))
      .withGracefulShutdownIgnoreSessions(Boolean.FALSE)
      .withGracefulShutdownWaitForSessions(Boolean.FALSE);

  protected static ClusteredServer CLUSTERED_SERVER_DEFAULTS =
    (new ClusteredServer())
      .withClusteredServerStartPolicy(CLUSTERED_SERVER_START_POLICY_IF_NEEDED);

  protected static NonClusteredServer NON_CLUSTERED_SERVER_DEFAULTS =
    (new NonClusteredServer())
      .withNonClusteredServerStartPolicy(NON_CLUSTERED_SERVER_START_POLICY_ALWAYS);

  protected static ClusterParams CLUSTER_DEFAULTS =
    (new ClusterParams())
      .withReplicas(null) // TBD - is this correct?
      .withMaxSurge(new IntOrString("20%")) // TBD - is this correct?
      .withMaxUnavailable(new IntOrString("20%")); // TBD - is this correct?

  private static final String[] SERVER_PROPERTY_NAMES = {
    "startedServerState",
    "restartedLabel",
    "nodePort",
    "env",
    "image",
    "imagePullPolicy",
    "imagePullSecrets",
    "shutdownPolicy",
    "gracefulShutdownTimeout",
    "gracefulShutdownIgnoreSessions",
    "gracefulShutdownWaitForSessions",
  };

  private static final String[] CLUSTERED_SERVER_ONLY_PROPERTY_NAMES = {
    "clusteredServerStartPolicy"
  };

  private static final String[] NON_CLUSTERED_SERVER_ONLY_PROPERTY_NAMES = {
    "nonClusteredServerStartPolicy"
  };

  private static final String[] CLUSTERED_SERVER_PROPERTY_NAMES =
    ArrayUtils.addAll(SERVER_PROPERTY_NAMES, CLUSTERED_SERVER_ONLY_PROPERTY_NAMES);

  private static final String[] NON_CLUSTERED_SERVER_PROPERTY_NAMES =
    ArrayUtils.addAll(SERVER_PROPERTY_NAMES, NON_CLUSTERED_SERVER_ONLY_PROPERTY_NAMES);

  private static final String[] CLUSTER_PROPERTY_NAMES = {
    "replicas",
    "maxSurge",
    "maxUnavailable"
  };

  /**
   * Gets the LifeCycleHelper singleton.
   * @return the lifecycle helper singleton
   */
  public static LifeCycleHelper instance() { return INSTANCE; }

  /**
   * Gets the effective configuration for a non-clustered server.
   * @param domainSpec the domain spec that the customer configured
   * @param serverName the name of the server
   * @return the effective configuration for the server
   */
  public NonClusteredServer getEffectiveNonClusteredServer(DomainSpec domainSpec, String serverName) {
    NonClusteredServer rtn = new NonClusteredServer();
    getEffectiveProperties(rtn, getNonClusteredServerParents(domainSpec, serverName), NON_CLUSTERED_SERVER_PROPERTY_NAMES);
    return rtn;
  }

  /**
   * Gets the effective configuration for a clustered server.
   * @param domainSpec the domain spec that the customer configured
   * @param clusterName the name of the cluster
   * @param serverName the name of the server
   * @return the effective configuration for the server
   */
  public ClusteredServer getEffectiveClusteredServer(DomainSpec domainSpec, String clusterName, String serverName) {
    ClusteredServer rtn = new ClusteredServer();
    getEffectiveProperties(rtn, getClusteredServerParents(domainSpec, clusterName, serverName), CLUSTERED_SERVER_PROPERTY_NAMES);
    return rtn;
  }

  /**
   * Gets the effective configuration for a cluster.
   * @param domainSpec the domain spec that the customer configured
   * @param clusterName the name of the cluster
   * @return the effective configuration for the cluster
   */
  public Cluster getEffectiveCluster(DomainSpec domainSpec, String clusterName) {
    Cluster rtn = new Cluster();
    getEffectiveProperties(rtn, getClusterParents(domainSpec, clusterName), CLUSTER_PROPERTY_NAMES);
    return rtn;
  }

  protected List<Object> getNonClusteredServerParents(DomainSpec domainSpec, String serverName) {
    List<Object> parents = new ArrayList();
    parents.add(domainSpec.getServers().get(serverName));
    parents.add(domainSpec.getNonClusteredServerDefaults());
    parents.add(domainSpec.getServerDefaults());
    parents.add(NON_CLUSTERED_SERVER_DEFAULTS);
    parents.add(SERVER_DEFAULTS);
    return parents;
  }

  protected List<Object> getClusteredServerParents(DomainSpec domainSpec, String clusterName, String serverName) {
    List<Object> parents = new ArrayList();
    Cluster cluster = domainSpec.getClusters().get(clusterName);
    if (cluster != null) {
      parents.add(cluster.getServers().get(serverName));
      parents.add(cluster.getServerDefaults());
    }
    ClusterParams clusterParams = domainSpec.getClusterDefaults();
    if (clusterParams != null) {
      parents.add(clusterParams.getServerDefaults());
    }
    parents.add(domainSpec.getServerDefaults());
    parents.add(CLUSTERED_SERVER_DEFAULTS);
    parents.add(SERVER_DEFAULTS);
    return parents;
  }

  protected List<Object> getClusterParents(DomainSpec domainSpec, String clusterName) {
    List<Object> parents = new ArrayList();
    parents.add(domainSpec.getClusters().get(clusterName));
    parents.add(domainSpec.getClusterDefaults());
    parents.add(CLUSTER_DEFAULTS);
    return parents;
  }

  protected void getEffectiveProperties(Object to, List<Object> parents, String... propertyNames) {
    BeanInfo toBI = getBeanInfo(to);
    for (Object parent : parents) {
      if (parent != null) {
        BeanInfo parentBI = getBeanInfo(parent);
        copyParentProperties(to, toBI, parent, parentBI, propertyNames);
      }
    }
  }

  protected void copyParentProperties(Object to, BeanInfo toBI, Object parent, BeanInfo parentBI, String... propertyNames) {
    for (String propertyName : propertyNames) {
      setPropertyIfUnsetAndHaveValue(to, toBI, propertyName, getProperty(parent, getPropertyDescriptor(parentBI, propertyName)));
    }
  }

  protected void setPropertyIfUnsetAndHaveValue(Object to, BeanInfo toBI, String propertyName, Object propertyValue) {
    if (propertyValue == null) {
      return; // we don't have a value to set
    }
    PropertyDescriptor pd = getPropertyDescriptor(toBI, propertyName);
    if (pd == null) {
      throw new AssertionError("Property does not exist: " + propertyName + ", " + toBI);
    }
    if (getProperty(to, pd) != null) {
      return; // the property has already be set
    }
    invoke(pd.getWriteMethod(), to, propertyValue);
  }

  protected Object getProperty(Object o, PropertyDescriptor pd) {
    if (pd == null) {
      return null; // the object does not have this property
    }
    return invoke(pd.getReadMethod(), o);
  }

  protected PropertyDescriptor getPropertyDescriptor(BeanInfo bi, String propertyName) {
    for (PropertyDescriptor pd : bi.getPropertyDescriptors()) {
      if (propertyName.equals(pd.getName())) {
        return pd;
      }
    }
    return null;
  }

  protected BeanInfo getBeanInfo(Object o) {
    try {
      return Introspector.getBeanInfo(o.getClass());
    } catch (Exception e) {
      throw new AssertionError(e);
    }
  }

  protected Object invoke(Method m, Object o, Object... args) {
    try {
      return m.invoke(o, args);
    } catch (Exception e) {
      throw new AssertionError(e);
    }
  }
}
