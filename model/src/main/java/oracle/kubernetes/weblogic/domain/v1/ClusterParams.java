// Copyright 2018, Oracle Corporation and/or its affiliates.  All rights reserved.
// Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.

package oracle.kubernetes.weblogic.domain.v1;

import javax.validation.Valid;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import io.kubernetes.client.custom.IntOrString;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;


/**
 * ClusterParams describes the desired state of a cluster.
 * 
 */
public class ClusterParams {

    /**
     * Replicas is the desired number of managed servers running in the WebLogic cluster.  Provided so that administrators can scale the Domain resource.
     * 
     */
    @SerializedName("replicas")
    @Expose
    private Integer replicas;
    /**
     * The maximum number of extra servers can be started when doing a rolling restart of the cluster. Value can be an absolute number (ex: 5) or a percentage of replicas (ex: 10%). This can not be 0 if MaxUnavailable is 0. Absolute number is calculated from percentage by rounding up.
     * 
     */
    @SerializedName("maxSurge")
    @Expose
    @Valid
    private IntOrString maxSurge;
    /**
     * The maximum number of servers that can be unavailable when doing restart of the cluster. Value can be an absolute number (ex: 5) or a percentage of replicas (ex: 10%). Absolute number is calculated from percentage by rounding down. This can not be 0 if MaxSurge is 0.
     * 
     */
    @SerializedName("maxUnavailable")
    @Expose
    @Valid
    private IntOrString maxUnavailable;
    /**
     * Default desired state of servers in this cluster.
     * 
     */
    @SerializedName("serverDefaults")
    @Expose
    private ClusteredServer serverDefaults;

    /**
     * Replicas is the desired number of managed servers running in the WebLogic cluster.  Provided so that administrators can scale the Domain resource.
     * @return replicas
     */
    public Integer getReplicas() {
        return replicas;
    }

    /**
     * Replicas is the desired number of managed servers running in the WebLogic cluster.  Provided so that administrators can scale the Domain resource.
     * @param replicas replicas
     */
    public void setReplicas(Integer replicas) {
        this.replicas = replicas;
    }

    /**
     * Replicas is the desired number of managed servers running in the WebLogic cluster.  Provided so that administrators can scale the Domain resource.
     * @param replicas replicas
     * @return this
     */
    public ClusterParams withReplicas(Integer replicas) {
        this.replicas = replicas;
        return this;
    }

    /**
     * The maximum number of extra servers can be started when doing a rolling restart of the cluster. Value can be an absolute number (ex: 5) or a percentage of replicas (ex: 10%). This can not be 0 if MaxUnavailable is 0. Absolute number is calculated from percentage by rounding up.
     * @return max surge
     */
    public IntOrString getMaxSurge() {
        return maxSurge;
    }

    /**
     * The maximum number of extra servers can be started when doing a rolling restart of the cluster. Value can be an absolute number (ex: 5) or a percentage of replicas (ex: 10%). This can not be 0 if MaxUnavailable is 0. Absolute number is calculated from percentage by rounding up.
     * @param maxSurge max surge
     */
    public void setMaxSurge(IntOrString maxSurge) {
        this.maxSurge = maxSurge;
    }

    /**
     * The maximum number of extra servers can be started when doing a rolling restart of the cluster. Value can be an absolute number (ex: 5) or a percentage of replicas (ex: 10%). This can not be 0 if MaxUnavailable is 0. Absolute number is calculated from percentage by rounding up.
     * @param maxSurge max surge
     * @return this
     */
    public ClusterParams withMaxSurge(IntOrString maxSurge) {
        this.maxSurge = maxSurge;
        return this;
    }

    /**
     * The maximum number of servers that can be unavailable when doing restart of the cluster. Value can be an absolute number (ex: 5) or a percentage of replicas (ex: 10%). Absolute number is calculated from percentage by rounding down. This can not be 0 if MaxSurge is 0.
     * @return max unavailable
     */
    public IntOrString getMaxUnavailable() {
        return maxUnavailable;
    }

    /**
     * The maximum number of servers that can be unavailable when doing restart of the cluster. Value can be an absolute number (ex: 5) or a percentage of replicas (ex: 10%). Absolute number is calculated from percentage by rounding down. This can not be 0 if MaxSurge is 0.
     * @param maxUnavailable max unavailable
     */
    public void setMaxUnavailable(IntOrString maxUnavailable) {
        this.maxUnavailable = maxUnavailable;
    }

    /**
     * The maximum number of servers that can be unavailable when doing restart of the cluster. Value can be an absolute number (ex: 5) or a percentage of replicas (ex: 10%). Absolute number is calculated from percentage by rounding down. This can not be 0 if MaxSurge is 0.
     * @param maxUnavailable max unavailable
     * @return this
     */
    public ClusterParams withMaxUnavailable(IntOrString maxUnavailable) {
        this.maxUnavailable = maxUnavailable;
        return this;
    }

    /**
     * Default desired state of servers in this cluster.
     * @return server defaults
     */
    public ClusteredServer getServerDefaults() {
        return serverDefaults;
    }

    /**
     * Default desired state of servers in this cluster.
     * @param serverDefaults server defaults
     */
    public void setServerDefaults(ClusteredServer serverDefaults) {
        this.serverDefaults = serverDefaults;
    }

    /**
     * Default desired state of servers in this cluster.
     * @param serverDefaults server defaults
     * @return this
     */
    public ClusterParams withServerDefaults(ClusteredServer serverDefaults) {
        this.serverDefaults = serverDefaults;
        return this;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).append("replicas", replicas).append("maxSurge", maxSurge).append("maxUnavailable", maxUnavailable).append("serverDefaults", serverDefaults).toString();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(maxSurge).append(serverDefaults).append(maxUnavailable).append(replicas).toHashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof ClusterParams) == false) {
            return false;
        }
        ClusterParams rhs = ((ClusterParams) other);
        return new EqualsBuilder().append(maxSurge, rhs.maxSurge).append(serverDefaults, rhs.serverDefaults).append(maxUnavailable, rhs.maxUnavailable).append(replicas, rhs.replicas).isEquals();
    }

}
