package ifmo.se.lab1app.system.kafka;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.kafka")
public class CreativeUploadKafkaProperties {

    private boolean enabled = true;
    private String bootstrapServers = "localhost:9092";
    private String uploadRequestTopic = "creative-upload-requests";
    private String uploadResultTopic = "creative-upload-results";
    private String workerGroupId = "creative-upload-workers";
    private String producerClientId = "lab1-app-outbox";
    private int outboxBatchSize = 25;
    private long outboxPollIntervalMs = 1000;
    private long producerAckTimeoutMs = 10000;
    private long workerPollTimeoutMs = 1000;
    private long workerRetryBackoffMs = 1000;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getBootstrapServers() {
        return bootstrapServers;
    }

    public void setBootstrapServers(String bootstrapServers) {
        this.bootstrapServers = bootstrapServers;
    }

    public String getUploadRequestTopic() {
        return uploadRequestTopic;
    }

    public void setUploadRequestTopic(String uploadRequestTopic) {
        this.uploadRequestTopic = uploadRequestTopic;
    }

    public String getUploadResultTopic() {
        return uploadResultTopic;
    }

    public void setUploadResultTopic(String uploadResultTopic) {
        this.uploadResultTopic = uploadResultTopic;
    }

    public String getWorkerGroupId() {
        return workerGroupId;
    }

    public void setWorkerGroupId(String workerGroupId) {
        this.workerGroupId = workerGroupId;
    }

    public String getProducerClientId() {
        return producerClientId;
    }

    public void setProducerClientId(String producerClientId) {
        this.producerClientId = producerClientId;
    }

    public int getOutboxBatchSize() {
        return outboxBatchSize;
    }

    public void setOutboxBatchSize(int outboxBatchSize) {
        this.outboxBatchSize = outboxBatchSize;
    }

    public long getOutboxPollIntervalMs() {
        return outboxPollIntervalMs;
    }

    public void setOutboxPollIntervalMs(long outboxPollIntervalMs) {
        this.outboxPollIntervalMs = outboxPollIntervalMs;
    }

    public long getProducerAckTimeoutMs() {
        return producerAckTimeoutMs;
    }

    public void setProducerAckTimeoutMs(long producerAckTimeoutMs) {
        this.producerAckTimeoutMs = producerAckTimeoutMs;
    }

    public long getWorkerPollTimeoutMs() {
        return workerPollTimeoutMs;
    }

    public void setWorkerPollTimeoutMs(long workerPollTimeoutMs) {
        this.workerPollTimeoutMs = workerPollTimeoutMs;
    }

    public long getWorkerRetryBackoffMs() {
        return workerRetryBackoffMs;
    }

    public void setWorkerRetryBackoffMs(long workerRetryBackoffMs) {
        this.workerRetryBackoffMs = workerRetryBackoffMs;
    }
}
