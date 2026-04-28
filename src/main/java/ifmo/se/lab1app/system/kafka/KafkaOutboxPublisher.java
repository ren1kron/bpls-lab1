package ifmo.se.lab1app.system.kafka;

import ifmo.se.lab1app.shared.application.TransactionExecutor;
import ifmo.se.lab1app.shared.domain.KafkaOutboxEvent;
import ifmo.se.lab1app.shared.infra.KafkaOutboxEventRepository;
import java.time.Duration;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.context.SmartLifecycle;

@Slf4j
@Component
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true", matchIfMissing = true)
public class KafkaOutboxPublisher implements SmartLifecycle {

    private final KafkaOutboxEventRepository outboxRepository;
    private final TransactionExecutor transactions;
    private final CreativeUploadKafkaProperties kafkaProperties;
    private final KafkaProducer<String, String> producer;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private Thread thread;

    public KafkaOutboxPublisher(
            KafkaOutboxEventRepository outboxRepository,
            TransactionExecutor transactions,
            CreativeUploadKafkaProperties kafkaProperties
    ) {
        this.outboxRepository = outboxRepository;
        this.transactions = transactions;
        this.kafkaProperties = kafkaProperties;
        this.producer = new KafkaProducer<>(producerProperties(kafkaProperties));
    }

    @Override
    public void start() {
        if (!running.compareAndSet(false, true)) {
            return;
        }
        thread = new Thread(this::publishLoop, "kafka-outbox-publisher");
        thread.start();
    }

    @Override
    public void stop() {
        running.set(false);
        if (thread != null) {
            thread.interrupt();
        }
        producer.close(Duration.ofSeconds(5));
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }

    private void publishLoop() {
        while (running.get()) {
            try {
                publishBatch();
                sleep(kafkaProperties.getOutboxPollIntervalMs());
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                running.set(false);
            } catch (Exception exception) {
                log.warn("Kafka outbox publish attempt failed", exception);
                sleepQuietly(kafkaProperties.getOutboxPollIntervalMs());
            }
        }
    }

    private void publishBatch() {
        transactions.write(() -> {
            List<KafkaOutboxEvent> events = outboxRepository.findUnpublishedForUpdate(
                    PageRequest.of(0, kafkaProperties.getOutboxBatchSize())
            );
            for (KafkaOutboxEvent event : events) {
                sendAndWaitForAck(event);
                event.markPublished();
            }
        });
    }

    private void sendAndWaitForAck(KafkaOutboxEvent event) {
        ProducerRecord<String, String> record = new ProducerRecord<>(
                event.getTopic(),
                event.getEventKey(),
                event.getPayload()
        );
        try {
            producer.send(record).get(kafkaProperties.getProducerAckTimeoutMs(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while publishing Kafka outbox event id=" + event.getId(), exception);
        } catch (ExecutionException | TimeoutException exception) {
            throw new IllegalStateException("Kafka broker did not acknowledge outbox event id=" + event.getId(), exception);
        }
    }

    private void sleep(long millis) throws InterruptedException {
        Thread.sleep(Math.max(1, millis));
    }

    private void sleepQuietly(long millis) {
        try {
            sleep(millis);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            running.set(false);
        }
    }

    private Properties producerProperties(CreativeUploadKafkaProperties kafkaProperties) {
        Properties properties = new Properties();
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaProperties.getBootstrapServers());
        properties.put(ProducerConfig.CLIENT_ID_CONFIG, kafkaProperties.getProducerClientId());
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.put(ProducerConfig.ACKS_CONFIG, "all");
        properties.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, "true");
        properties.put(ProducerConfig.RETRIES_CONFIG, Integer.toString(Integer.MAX_VALUE));
        return properties;
    }
}
