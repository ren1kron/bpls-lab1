package ifmo.se.lab1app.camunda;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.camunda")
public class CamundaProperties {

    private boolean enabled;
    private boolean identitySyncEnabled = true;
    private boolean externalTaskWorkerEnabled = true;
    private String baseUrl = "http://localhost:8080/engine-rest";
    private String username = "demo";
    private String password = "demo";
    private String workerId = "lab1-api-worker";
    private int maxTasks = 8;
    private long asyncResponseTimeoutMs = 1000;
    private long lockDurationMs = 30000;
    private int failureRetries = 3;
    private long retryTimeoutMs = 10000;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isIdentitySyncEnabled() {
        return identitySyncEnabled;
    }

    public void setIdentitySyncEnabled(boolean identitySyncEnabled) {
        this.identitySyncEnabled = identitySyncEnabled;
    }

    public boolean isExternalTaskWorkerEnabled() {
        return externalTaskWorkerEnabled;
    }

    public void setExternalTaskWorkerEnabled(boolean externalTaskWorkerEnabled) {
        this.externalTaskWorkerEnabled = externalTaskWorkerEnabled;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getWorkerId() {
        return workerId;
    }

    public void setWorkerId(String workerId) {
        this.workerId = workerId;
    }

    public int getMaxTasks() {
        return maxTasks;
    }

    public void setMaxTasks(int maxTasks) {
        this.maxTasks = maxTasks;
    }

    public long getAsyncResponseTimeoutMs() {
        return asyncResponseTimeoutMs;
    }

    public void setAsyncResponseTimeoutMs(long asyncResponseTimeoutMs) {
        this.asyncResponseTimeoutMs = asyncResponseTimeoutMs;
    }

    public long getLockDurationMs() {
        return lockDurationMs;
    }

    public void setLockDurationMs(long lockDurationMs) {
        this.lockDurationMs = lockDurationMs;
    }

    public int getFailureRetries() {
        return failureRetries;
    }

    public void setFailureRetries(int failureRetries) {
        this.failureRetries = failureRetries;
    }

    public long getRetryTimeoutMs() {
        return retryTimeoutMs;
    }

    public void setRetryTimeoutMs(long retryTimeoutMs) {
        this.retryTimeoutMs = retryTimeoutMs;
    }
}
