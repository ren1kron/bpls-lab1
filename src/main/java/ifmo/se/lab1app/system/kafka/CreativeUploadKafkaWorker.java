package ifmo.se.lab1app.system.kafka;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true", matchIfMissing = true)
public class CreativeUploadKafkaWorker implements SmartLifecycle {

    private final CreativeUploadKafkaProperties kafkaProperties;
    private final CreativeUploadSagaHandler sagaHandler;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private Thread thread;
    private KafkaConsumer<String, String> consumer;

    public CreativeUploadKafkaWorker(
            CreativeUploadKafkaProperties kafkaProperties,
            CreativeUploadSagaHandler sagaHandler
    ) {
        this.kafkaProperties = kafkaProperties;
        this.sagaHandler = sagaHandler;
    }

    @Override
    public void start() {
        if (!running.compareAndSet(false, true)) {
            return;
        }
        thread = new Thread(this::consumeLoop, "creative-upload-kafka-worker");
        thread.start();
    }

    @Override
    public void stop() {
        running.set(false);
        if (consumer != null) {
            consumer.wakeup();
        }
        if (thread != null) {
            thread.interrupt();
        }
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }

    private void consumeLoop() {
        try (KafkaConsumer<String, String> kafkaConsumer = new KafkaConsumer<>(consumerProperties(kafkaProperties))) {
            consumer = kafkaConsumer;
            kafkaConsumer.subscribe(List.of(kafkaProperties.getUploadRequestTopic()));

            while (running.get()) {
                for (ConsumerRecord<String, String> record : kafkaConsumer.poll(
                        Duration.ofMillis(kafkaProperties.getWorkerPollTimeoutMs())
                )) {
                    consume(record, kafkaConsumer);
                }
            }
        } catch (org.apache.kafka.common.errors.WakeupException exception) {
            if (running.get()) {
                throw exception;
            }
        } finally {
            running.set(false);
        }
    }

    private void consume(ConsumerRecord<String, String> record, KafkaConsumer<String, String> kafkaConsumer) {
        try {
            sagaHandler.handleRequestPayload(record.value());
            commitOffset(record, kafkaConsumer);
            log.info(
                    "Processed creative upload record topic={} partition={} offset={}",
                    record.topic(),
                    record.partition(),
                    record.offset()
            );
        } catch (IllegalArgumentException exception) {
            log.error("Skipping malformed creative upload event at offset={}", record.offset(), exception);
            commitOffset(record, kafkaConsumer);
        } catch (Exception exception) {
            log.error("Creative upload event failed; offset will be retried offset={}", record.offset(), exception);
            sleepQuietly(kafkaProperties.getWorkerRetryBackoffMs());
        }
    }

    private void commitOffset(ConsumerRecord<String, String> record, KafkaConsumer<String, String> kafkaConsumer) {
        TopicPartition topicPartition = new TopicPartition(record.topic(), record.partition());
        OffsetAndMetadata offset = new OffsetAndMetadata(record.offset() + 1);
        kafkaConsumer.commitSync(Collections.unmodifiableMap(Map.of(topicPartition, offset)));
    }

    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(Math.max(1, millis));
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            running.set(false);
        }
    }

    private Properties consumerProperties(CreativeUploadKafkaProperties kafkaProperties) {
        Properties properties = new Properties();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaProperties.getBootstrapServers());
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, kafkaProperties.getWorkerGroupId());
        properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        return properties;
    }
}
