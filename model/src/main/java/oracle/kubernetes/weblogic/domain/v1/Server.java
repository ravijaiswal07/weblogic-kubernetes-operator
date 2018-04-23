// Copyright 2018, Oracle Corporation and/or its affiliates.  All rights reserved.
// Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.

package oracle.kubernetes.weblogic.domain.v1;

import java.util.ArrayList;
import java.util.List;
import javax.validation.Valid;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import io.kubernetes.client.models.V1EnvVar;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;


/**
 * Server describes the desired state of a server.
 * 
 */
public class Server {

    /**
     * Desired startup state.  Legal values are RUNNING and ADMIN.
     * 
     */
    @SerializedName("startedServerState")
    @Expose
    private String startedServerState;
    /**
     * Indicates that a server has been restarted.  If a running server's pod does not have this label, then the operator needs to restart the server and attach this label to it.
     * 
     */
    @SerializedName("restartedLabel")
    @Expose
    private String restartedLabel;
    /**
     * NodePort for the server.  The port on each node on which this managed server will be exposed.  If specified, this value must be an unused port.  By default, the server will not be exposed outside the Kubernetes cluster.
     * 
     */
    @SerializedName("nodePort")
    @Expose
    private Integer nodePort;
    /**
     * Environment variables to pass while starting this server.  If not specified, then the environment variables in config.xml will be used instead.
     * 
     */
    @SerializedName("env")
    @Expose
    @Valid
    private List<V1EnvVar> env = new ArrayList<V1EnvVar>();
    /**
     * WebLogic Docker image.  Defaults to store/oracle/weblogic:12.2.1.3
     * 
     */
    @SerializedName("image")
    @Expose
    private String image;
    /**
     * Image pull policy. Legal values are Always, Never and IfNotPresent. Defaults to Always if :latest tag is specified, or IfNotPresent otherwise. Cannot be updated. More info: https://kubernetes.io/docs/concepts/containers/images#updating-images
     * 
     */
    @SerializedName("imagePullPolicy")
    @Expose
    private String imagePullPolicy;
    /**
     * The name of a kubernetes secrets object that contains the credentials needed to pull the WebLogic Docker image.
     * 
     */
    @SerializedName("imagePullSecrets")
    @Expose
    private String imagePullSecrets;
    /**
     * Controls how the operator will stop this server.  Legal values are GRACEFUL_SHUTDOWN and FORCED_SHUTDOWN.
     * 
     */
    @SerializedName("shutdownPolicy")
    @Expose
    private String shutdownPolicy;
    /**
     * Number of seconds to wait before aborting inflight work and force shutting down the server.
     * 
     */
    @SerializedName("gracefulShutdownTimeout")
    @Expose
    private Integer gracefulShutdownTimeout;
    /**
     * Set to <code>true</code> to ignore pending HTTP sessions during inflight work handling.
     * 
     */
    @SerializedName("gracefulShutdownIgnoreSessions")
    @Expose
    private Boolean gracefulShutdownIgnoreSessions;
    /**
     * Set to <code>true</code> to wait for all HTTP sessions during inflight work handling; <code>false</code> to wait for non-persisted.
     * 
     */
    @SerializedName("gracefulShutdownWaitForSessions")
    @Expose
    private Boolean gracefulShutdownWaitForSessions;

    /**
     * Desired startup state.  Legal values are RUNNING and ADMIN.
     * @return started server state
     */
    public String getStartedServerState() {
        return startedServerState;
    }

    /**
     * Desired startup state.  Legal values are RUNNING and ADMIN.
     * @param startedServerState started server state
     */
    public void setStartedServerState(String startedServerState) {
        this.startedServerState = startedServerState;
    }

    /**
     * Desired startup state.  Legal values are RUNNING and ADMIN.
     * @param startedServerState started server state
     * @return this
     */
    public Server withStartedServerState(String startedServerState) {
        this.startedServerState = startedServerState;
        return this;
    }

    /**
     * Indicates that a server has been restarted.  If a running server's pod does not have this label, then the operator needs to restart the server and attach this label to it.
     * @return restarted label
     */
    public String getRestartedLabel() {
        return restartedLabel;
    }

    /**
     * Indicates that a server has been restarted.  If a running server's pod does not have this label, then the operator needs to restart the server and attach this label to it.
     * @param restartedLabel restarted label
     */
    public void setRestartedLabel(String restartedLabel) {
        this.restartedLabel = restartedLabel;
    }

    /**
     * Indicates that a server has been restarted.  If a running server's pod does not have this label, then the operator needs to restart the server and attach this label to it.
     * @param restartedLabel restarted label
     * @return this
     */
    public Server withRestartedLabel(String restartedLabel) {
        this.restartedLabel = restartedLabel;
        return this;
    }

    /**
     * NodePort for the server.  The port on each node on which this managed server will be exposed.  If specified, this value must be an unused port.  By default, the server will not be exposed outside the Kubernetes cluster.
     * @return node port
     */
    public Integer getNodePort() {
        return nodePort;
    }

    /**
     * NodePort for the server.  The port on each node on which this managed server will be exposed.  If specified, this value must be an unused port.  By default, the server will not be exposed outside the Kubernetes cluster.
     * @param nodePort node port
     */
    public void setNodePort(Integer nodePort) {
        this.nodePort = nodePort;
    }

    /**
     * NodePort for the server.  The port on each node on which this managed server will be exposed.  If specified, this value must be an unused port.  By default, the server will not be exposed outside the Kubernetes cluster.
     * @param nodePort node port
     */
    public Server withNodePort(Integer nodePort) {
        this.nodePort = nodePort;
        return this;
    }

    /**
     * Environment variables to pass while starting this server.  If not specified, then the environment variables in config.xml will be used instead.
     * @return env
     */
    public List<V1EnvVar> getEnv() {
        return env;
    }

    /**
     * Environment variables to pass while starting this server.  If not specified, then the environment variables in config.xml will be used instead.
     * @param env env
     */
    public void setEnv(List<V1EnvVar> env) {
        this.env = env;
    }

    /**
     * Environment variables to pass while starting this server.  If not specified, then the environment variables in config.xml will be used instead.
     * @param env env
     * @return this
     */
    public Server withEnv(List<V1EnvVar> env) {
        this.env = env;
        return this;
    }

    /**
     * WebLogic Docker image.  Defaults to store/oracle/weblogic:12.2.1.3
     * @return image
     */
    public String getImage() {
        return image;
    }

    /**
     * WebLogic Docker image.  Defaults to store/oracle/weblogic:12.2.1.3
     * @param image image
     */
    public void setImage(String image) {
        this.image = image;
    }

    /**
     * WebLogic Docker image.  Defaults to store/oracle/weblogic:12.2.1.3
     * @param image image
     * @return this
     */
    public Server withImage(String image) {
        this.image = image;
        return this;
    }

    /**
     * Image pull policy. Legal values are Always, Never and IfNotPresent. Defaults to Always if :latest tag is specified, or IfNotPresent otherwise. Cannot be updated. More info: https://kubernetes.io/docs/concepts/containers/images#updating-images
     * @return image pull policy
     */
    public String getImagePullPolicy() {
        return imagePullPolicy;
    }

    /**
     * Image pull policy. Legal values are Always, Never and IfNotPresent. Defaults to Always if :latest tag is specified, or IfNotPresent otherwise. Cannot be updated. More info: https://kubernetes.io/docs/concepts/containers/images#updating-images
     * @param imagePullPolicy image pull policy
     */
    public void setImagePullPolicy(String imagePullPolicy) {
        this.imagePullPolicy = imagePullPolicy;
    }

    /**
     * Image pull policy. Legal values are Always, Never and IfNotPresent. Defaults to Always if :latest tag is specified, or IfNotPresent otherwise. Cannot be updated. More info: https://kubernetes.io/docs/concepts/containers/images#updating-images
     * @param imagePullPolicy image pull policy
     * @return this
     */
    public Server withImagePullPolicy(String imagePullPolicy) {
        this.imagePullPolicy = imagePullPolicy;
        return this;
    }

    /**
     * The name of a kubernetes secrets object that contains the credentials needed to pull the WebLogic Docker image.
     * @return image pull secrets
     */
    public String getImagePullSecrets() {
        return imagePullSecrets;
    }

    /**
     * The name of a kubernetes secrets object that contains the credentials needed to pull the WebLogic Docker image.
     * @param image pull secrets
     */
    public void setImagePullSecrets(String imagePullSecrets) {
        this.imagePullSecrets = imagePullSecrets;
    }

    /**
     * The name of a kubernetes secrets object that contains the credentials needed to pull the WebLogic Docker image.
     * @param image pull secrets
     * @return this
     */
    public Server withImagePullSecrets(String imagePullSecrets) {
        this.imagePullSecrets = imagePullSecrets;
        return this;
    }

    /**
     * Controls how the operator will stop this server.  Legal values are GRACEFUL_SHUTDOWN and FORCED_SHUTDOWN.
     * @return shutdown policy
     */
    public String getShutdownPolicy() {
        return shutdownPolicy;
    }

    /**
     * Controls how the operator will stop this server.  Legal values are GRACEFUL_SHUTDOWN and FORCED_SHUTDOWN.
     * @param shutdownPolicy shutdown policy
     */
    public void setShutdownPolicy(String shutdownPolicy) {
        this.shutdownPolicy = shutdownPolicy;
    }

    /**
     * Controls how the operator will stop this server.  Legal values are GRACEFUL_SHUTDOWN and FORCED_SHUTDOWN.
     * @param shutdownPolicy shutdown policy
     * @return this
     */
    public Server withShutdownPolicy(String shutdownPolicy) {
        this.shutdownPolicy = shutdownPolicy;
        return this;
    }

    /**
     * Number of seconds to wait before aborting inflight work and force shutting down the server.
     * @return graceful shutdown timeout
     */
    public Integer getGracefulShutdownTimeout() {
        return gracefulShutdownTimeout;
    }

    /**
     * Number of seconds to wait before aborting inflight work and force shutting down the server.
     * @param gracefulShutdownTimeout graceful timeout timeout
     */
    public void setGracefulShutdownTimeout(Integer gracefulShutdownTimeout) {
        this.gracefulShutdownTimeout = gracefulShutdownTimeout;
    }

    /**
     * Number of seconds to wait before aborting inflight work and force shutting down the server.
     * @param gracefulShutdownTimeout graceful timeout timeout
     * @return this
     */
    public Server withGracefulShutdownTimeout(Integer gracefulShutdownTimeout) {
        this.gracefulShutdownTimeout = gracefulShutdownTimeout;
        return this;
    }

    /**
     * Set to <code>true</code> to ignore pending HTTP sessions during inflight work handling.
     * @return graceful shutdown ignore sessions
     */
    public Boolean getGracefulShutdownIgnoreSessions() {
        return gracefulShutdownIgnoreSessions;
    }

    /**
     * Set to <code>true</code> to ignore pending HTTP sessions during inflight work handling.
     * @parama gracefulShutdownIgnoreSessions graceful shutdown ignore sessions
     */
    public void setGracefulShutdownIgnoreSessions(Boolean gracefulShutdownIgnoreSessions) {
        this.gracefulShutdownIgnoreSessions = gracefulShutdownIgnoreSessions;
    }

    /**
     * Set to <code>true</code> to ignore pending HTTP sessions during inflight work handling.
     * @parama gracefulShutdownIgnoreSessions graceful shutdown ignore sessions
     * @return this
     */
    public Server withGracefulShutdownIgnoreSessions(Boolean gracefulShutdownIgnoreSessions) {
        this.gracefulShutdownIgnoreSessions = gracefulShutdownIgnoreSessions;
        return this;
    }

    /**
     * Set to <code>true</code> to wait for all HTTP sessions during inflight work handling; <code>false</code> to wait for non-persisted.
     * @return graceful shutdown wait for sessions
     */
    public Boolean getGracefulShutdownWaitForSessions() {
        return gracefulShutdownWaitForSessions;
    }

    /**
     * Set to <code>true</code> to wait for all HTTP sessions during inflight work handling; <code>false</code> to wait for non-persisted.
     * @param gracefulShutdownWaitForSessions graceful shutdown wait for sessions
     */
    public void setGracefulShutdownWaitForSessions(Boolean gracefulShutdownWaitForSessions) {
        this.gracefulShutdownWaitForSessions = gracefulShutdownWaitForSessions;
    }

    /**
     * Set to <code>true</code> to wait for all HTTP sessions during inflight work handling; <code>false</code> to wait for non-persisted.
     * @param gracefulShutdownWaitForSessions graceful shutdown wait for sessions
     * @return this
     */
    public Server withGracefulShutdownWaitForSessions(Boolean gracefulShutdownWaitForSessions) {
        this.gracefulShutdownWaitForSessions = gracefulShutdownWaitForSessions;
        return this;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).append("startedServerState", startedServerState).append("restartedLabel", restartedLabel).append("nodePort", nodePort).append("env", env).append("image", image).append("imagePullPolicy", imagePullPolicy).append("imagePullSecrets", imagePullSecrets).append("shutdownPolicy", shutdownPolicy).append("gracefulShutdownTimeout", gracefulShutdownTimeout).append("gracefulShutdownIgnoreSessions", gracefulShutdownIgnoreSessions).append("gracefulShutdownWaitForSessions", gracefulShutdownWaitForSessions).toString();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(gracefulShutdownTimeout).append(image).append(imagePullPolicy).append(startedServerState).append(imagePullSecrets).append(restartedLabel).append(gracefulShutdownIgnoreSessions).append(env).append(gracefulShutdownWaitForSessions).append(nodePort).append(shutdownPolicy).toHashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof Server) == false) {
            return false;
        }
        Server rhs = ((Server) other);
        return new EqualsBuilder().append(gracefulShutdownTimeout, rhs.gracefulShutdownTimeout).append(image, rhs.image).append(imagePullPolicy, rhs.imagePullPolicy).append(startedServerState, rhs.startedServerState).append(imagePullSecrets, rhs.imagePullSecrets).append(restartedLabel, rhs.restartedLabel).append(gracefulShutdownIgnoreSessions, rhs.gracefulShutdownIgnoreSessions).append(env, rhs.env).append(gracefulShutdownWaitForSessions, rhs.gracefulShutdownWaitForSessions).append(nodePort, rhs.nodePort).append(shutdownPolicy, rhs.shutdownPolicy).isEquals();
    }

}
