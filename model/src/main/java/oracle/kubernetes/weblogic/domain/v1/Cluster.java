// Copyright 2018, Oracle Corporation and/or its affiliates.  All rights reserved.
// Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.

package oracle.kubernetes.weblogic.domain.v1;

import java.util.HashMap;
import java.util.Map;
import javax.validation.Valid;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import io.kubernetes.client.custom.IntOrString;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;


/**
 * Cluster describes the desired state of a cluster.
 * 
 */
public class Cluster
    extends ClusterParams
{

    /**
     * Maps the name of a server in this cluster to its desired state.
     * 
     */
    @SerializedName("servers")
    @Expose
    @Valid
    private Map<String, ClusteredServer> servers = new HashMap<String, ClusteredServer>();
    /**
     * Maps the name of a server in this cluster to its desired state.
     * @return servers
     */
    public  Map<String, ClusteredServer> getServers() {
        return this.servers;
    }

    /**
     * Maps the name of a server in this cluster to its desired state.
     * @param clusters clusters
     */
    public void setServers(Map<String, ClusteredServer> servers) {
        this.servers = servers;
    }
    /**
     * Maps the name of a server in this cluster to its desired state.
     * @param clusters clusters
     * @return this
     */
    public Cluster withServers(Map<String, ClusteredServer> servers) {
        this.servers = servers;
        return this;
    }

    /**
     * Maps the name of a server in this cluster to its desired state.
     * @param name cluster name
     * @param cluster cluster
     */
    public void setServer(String name, ClusteredServer server) {
        this.servers.put(name, server);
    }

    /**
     * Maps the name of a server in this cluster to its desired state.
     * @param name name
     * @param cluster cluster
     * @return this
     */
    public Cluster withServer(String name, ClusteredServer server) {
        this.servers.put(name, server);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Cluster withReplicas(Integer replicas) {
        super.withReplicas(replicas);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Cluster withMaxSurge(IntOrString maxSurge) {
        super.withMaxSurge(maxSurge);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Cluster withMaxUnavailable(IntOrString maxUnavailable) {
        super.withMaxUnavailable(maxUnavailable);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Cluster withServerDefaults(ClusteredServer serverDefaults) {
        super.withServerDefaults(serverDefaults);
        return this;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).appendSuper(super.toString()).append("servers", servers).toString();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().appendSuper(super.hashCode()).append(servers).toHashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof Cluster) == false) {
            return false;
        }
        Cluster rhs = ((Cluster) other);
        return new EqualsBuilder().appendSuper(super.equals(other)).append(servers, rhs.servers).isEquals();
    }

}
