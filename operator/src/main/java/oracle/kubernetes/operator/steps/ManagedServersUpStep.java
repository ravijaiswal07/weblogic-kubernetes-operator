// Copyright 2017, 2018, Oracle Corporation and/or its affiliates.  All rights reserved.
// Licensed under the Universal Permissive License v 1.0 as shown at
// http://oss.oracle.com/licenses/upl.

package oracle.kubernetes.operator.steps;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import oracle.kubernetes.operator.helpers.ClusterConfig;
import oracle.kubernetes.operator.helpers.ClusteredServerConfig;
import oracle.kubernetes.operator.helpers.DomainConfig;
import oracle.kubernetes.operator.helpers.DomainPresenceInfo;
import oracle.kubernetes.operator.helpers.DomainPresenceInfo.ServerStartupInfo;
import oracle.kubernetes.operator.helpers.LifeCycleHelper;
import oracle.kubernetes.operator.helpers.NonClusteredServerConfig;
import oracle.kubernetes.operator.helpers.ServerKubernetesObjects;
import oracle.kubernetes.operator.logging.LoggingFacade;
import oracle.kubernetes.operator.logging.LoggingFactory;
import oracle.kubernetes.operator.wlsconfig.WlsClusterConfig;
import oracle.kubernetes.operator.wlsconfig.WlsDomainConfig;
import oracle.kubernetes.operator.wlsconfig.WlsServerConfig;
import oracle.kubernetes.operator.work.NextAction;
import oracle.kubernetes.operator.work.Packet;
import oracle.kubernetes.operator.work.Step;
import oracle.kubernetes.weblogic.domain.v1.Domain;
import oracle.kubernetes.weblogic.domain.v1.DomainSpec;

public class ManagedServersUpStep extends Step {
  private static final LoggingFacade LOGGER = LoggingFactory.getLogger("Operator", "Operator");

  public ManagedServersUpStep(Step next) {
    super(next);
  }

  @Override
  public NextAction apply(Packet packet) {
    LOGGER.entering();
    DomainPresenceInfo info = packet.getSPI(DomainPresenceInfo.class);

    Domain dom = info.getDomain();
    DomainSpec spec = dom.getSpec();

    if (LOGGER.isFineEnabled()) {
      Collection<String> runningList = new ArrayList<>();
      for (Map.Entry<String, ServerKubernetesObjects> entry : info.getServers().entrySet()) {
        ServerKubernetesObjects sko = entry.getValue();
        if (sko != null && sko.getPod() != null) {
          runningList.add(entry.getKey());
        }
      }
      LOGGER.fine(
          "Running servers for domain with UID: "
              + spec.getDomainUID()
              + ", running list: "
              + runningList);
    }

    WlsDomainConfig scan = info.getScan();
    DomainConfig domainConfig =
        LifeCycleHelper.instance()
            .getEffectiveDomainConfig(
                dom, scan.getStandaloneServerConfigs().keySet(), scan.getClusters());

    processClusterRestarts(info, scan);

    Collection<ServerStartupInfo> ssic = new ArrayList<ServerStartupInfo>();
    String asName = spec.getAsName();
    Collection<String> servers = new ArrayList<String>();
    Collection<String> clusters = new ArrayList<String>();

    processNonClusteredServers(scan, domainConfig, ssic, asName, servers);

    processClusters(scan, domainConfig, ssic, asName, servers, clusters);

    info.setServerStartupInfo(ssic);
    LOGGER.exiting();
    return doNext(
        stopServersThatShouldNotBeRunning(
            info,
            domainConfig,
            servers,
            new ClusterServicesStep(info, new ManagedServerUpIteratorStep(ssic, getNext()))),
        packet);
  }

  protected static void processClusterRestarts(DomainPresenceInfo info, WlsDomainConfig scan) {
    for (String clusterName : info.getExplicitRestartClusters()) {
      WlsClusterConfig cluster = scan.getClusterConfig(clusterName);
      if (cluster != null) {
        for (WlsServerConfig server : cluster.getServerConfigs()) {
          info.getExplicitRestartServers().add(server.getName());
        }
      }
    }
    info.getExplicitRestartClusters().clear();
  }

  protected static void processClusters(
      WlsDomainConfig scan,
      DomainConfig domainConfig,
      Collection<ServerStartupInfo> ssic,
      String asName,
      Collection<String> servers,
      Collection<String> clusters) {
    // Go through all clusters
    Map<String, ClusterConfig> clusterConfigs = domainConfig.getClusters();
    if (clusterConfigs != null) {
      cluster:
      for (Map.Entry<String, ClusterConfig> entry : clusterConfigs.entrySet()) {
        String clusterName = entry.getKey();
        ClusterConfig clusterConfig = entry.getValue();
        clusters.add(clusterName);
        // find cluster
        WlsClusterConfig wlsClusterConfig = scan.getClusterConfig(clusterName);
        if (wlsClusterConfig != null) {
          int startedCount = 0;
          List ifNeededList = new ArrayList();
          for (Map.Entry<String, ClusteredServerConfig> clusteredServer :
              clusterConfig.getServers().entrySet()) {
            String serverName = clusteredServer.getKey();
            ClusteredServerConfig clusteredServerConfig = clusteredServer.getValue();
            if (ClusteredServerConfig.CLUSTERED_SERVER_START_POLICY_IF_NEEDED.equals(
                clusteredServerConfig.getClusteredServerStartPolicy())) {
              ifNeededList.add(clusteredServerConfig);
              continue;
            }
            if (ClusteredServerConfig.CLUSTERED_SERVER_START_POLICY_ALWAYS.equals(
                clusteredServerConfig.getClusteredServerStartPolicy())) {

              startedCount =
                  addClusteredServer(
                      scan,
                      ssic,
                      asName,
                      servers,
                      wlsClusterConfig,
                      startedCount,
                      serverName,
                      clusteredServerConfig);
            }
          } // end for Clustered Servers
          // Now process Clustered Servers that have Clustered Server Start Policy of 'If Needed'
          if (!ifNeededList.isEmpty()) {
            Iterator<ClusteredServerConfig> itr = ifNeededList.iterator();
            while (itr.hasNext() && startedCount < clusterConfig.getReplicas()) {
              ClusteredServerConfig clusteredServerConfig = itr.next();
              String serverName = clusteredServerConfig.getServerName();
              startedCount =
                  addClusteredServer(
                      scan,
                      ssic,
                      asName,
                      servers,
                      wlsClusterConfig,
                      startedCount,
                      serverName,
                      clusteredServerConfig);
            }
          }
        }
      } // end for Cluster
    }
  }

  protected static int addClusteredServer(
      WlsDomainConfig scan,
      Collection<ServerStartupInfo> ssic,
      String asName,
      Collection<String> servers,
      WlsClusterConfig wlsClusterConfig,
      int startedCount,
      String serverName,
      ClusteredServerConfig clusteredServerConfig) {
    if (!serverName.equals(asName) && !servers.contains(serverName)) {
      // start server
      servers.add(serverName);
      WlsServerConfig wlsServerConfig = wlsClusterConfig.getServerConfig(serverName);
      ssic.add(new ServerStartupInfo(wlsServerConfig, wlsClusterConfig, clusteredServerConfig));
      startedCount++;
    }
    return startedCount;
  }

  protected static void processNonClusteredServers(
      WlsDomainConfig scan,
      DomainConfig domainConfig,
      Collection<ServerStartupInfo> ssic,
      String asName,
      Collection<String> servers) {
    // start non clustered (standalone) servers
    Map<String, NonClusteredServerConfig> nonClusteredServers = domainConfig.getServers();
    if (nonClusteredServers != null) {
      for (Map.Entry<String, NonClusteredServerConfig> entry : nonClusteredServers.entrySet()) {
        String serverName = entry.getKey();
        NonClusteredServerConfig nonClusteredServer = entry.getValue();
        if (nonClusteredServer
            .getNonClusteredServerStartPolicy()
            .equals(NonClusteredServerConfig.NON_CLUSTERED_SERVER_START_POLICY_ALWAYS)) {
          WlsServerConfig wlsServerConfig = scan.getServerConfig(serverName);
          if (!serverName.equals(asName)
              && wlsServerConfig != null
              && !servers.contains(serverName)) {
            // start server
            servers.add(serverName);
            ssic.add(new ServerStartupInfo(wlsServerConfig, null, nonClusteredServer));
          }
        }
      }
    }
  }

  // This method stops any running servers that should not be running.
  // For example, when a cluster is scaled down, this method will stop
  // any extra running servers for that cluster.
  // TBD - split this into smaller unit testable methods ...
  private static Step stopServersThatShouldNotBeRunning(
      DomainPresenceInfo info,
      DomainConfig domainConfig,
      Collection<String> managedServersThatShouldBeRunning,
      Step next) {
    Domain domain = info.getDomain();
    DomainSpec domainSpec = domain.getSpec();

    Map<String, ServerKubernetesObjects> currentServers = info.getServers();
    Collection<Map.Entry<String, ServerKubernetesObjects>> serversToStop = new ArrayList<>();

    // Since the admin server is started separately from the managed servers,
    // it does not get put on the list of managed servers that should be running.
    // Therefore, we need to find out explicitly whether or not it should be running.
    String adminServerName = domainSpec.getAsName();
    boolean shouldStopAdmin = shouldStopAdminServer(adminServerName, domainConfig);

    for (Map.Entry<String, ServerKubernetesObjects> entry : currentServers.entrySet()) {
      String serverName = entry.getKey();
      if (shouldStopServer(
          serverName, adminServerName, shouldStopAdmin, managedServersThatShouldBeRunning)) {
        serversToStop.add(entry);
      }
    }

    if (!serversToStop.isEmpty()) {
      return new ServerDownIteratorStep(serversToStop, next);
    }

    return next;
  }

  protected static boolean shouldStopAdminServer(
      String adminServerName, DomainConfig domainConfig) {
    // TBD - the admin server could be a managed server
    NonClusteredServerConfig asServerConfig = domainConfig.getServers().get(adminServerName);
    if (asServerConfig != null
        && asServerConfig
            .getNonClusteredServerStartPolicy()
            .equals(NonClusteredServerConfig.NON_CLUSTERED_SERVER_START_POLICY_NEVER)) {
      return true;
    }
    return false;
  }

  protected static boolean shouldStopServer(
      String serverName,
      String adminServerName,
      boolean shouldStopAdminServer,
      Collection<String> managedServersThatShouldBeRunning) {
    if (adminServerName.equals(serverName) && shouldStopAdminServer) {
      return true;
    }
    if (!managedServersThatShouldBeRunning.contains(serverName)) {
      return true;
    }
    return false;
  }
}
