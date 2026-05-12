package ifmo.se.lab1app.eis;

import ifmo.se.lab1app.shared.application.TransactionExecutor;
import ifmo.se.lab1app.shared.domain.EisOutboxEvent;
import ifmo.se.lab1app.shared.infra.EisOutboxEventRepository;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.SmartLifecycle;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(name = "app.eis.enabled", havingValue = "true")
public class EisOutboxPublisher implements SmartLifecycle {

    private final EisOutboxEventRepository outboxRepository;
    private final TransactionExecutor transactions;
    private final CampaignEisEventPublisher eventPublisher;
    private final EisIntegrationProperties properties;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private Thread thread;

    public EisOutboxPublisher(
            EisOutboxEventRepository outboxRepository,
            TransactionExecutor transactions,
            CampaignEisEventPublisher eventPublisher,
            EisIntegrationProperties properties
    ) {
        this.outboxRepository = outboxRepository;
        this.transactions = transactions;
        this.eventPublisher = eventPublisher;
        this.properties = properties;
    }

    @Override
    public void start() {
        if (!running.compareAndSet(false, true)) {
            return;
        }
        thread = new Thread(this::publishLoop, "eis-outbox-publisher");
        thread.start();
    }

    @Override
    public void stop() {
        running.set(false);
        if (thread != null) {
            thread.interrupt();
        }
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }

    void publishBatch() {
        transactions.write(() -> {
            List<EisOutboxEvent> events = outboxRepository.findUnpublishedForUpdate(
                    PageRequest.of(0, properties.outboxBatchSize())
            );
            for (EisOutboxEvent event : events) {
                eventPublisher.publishStoredPayload(event.getPayload());
                event.markPublished();
            }
        });
    }

    private void publishLoop() {
        while (running.get()) {
            try {
                publishBatch();
                sleep(properties.outboxPollIntervalMs());
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                running.set(false);
            } catch (Exception exception) {
                log.warn("EIS outbox publish attempt failed", exception);
                sleepQuietly(properties.outboxPollIntervalMs());
            }
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
}
