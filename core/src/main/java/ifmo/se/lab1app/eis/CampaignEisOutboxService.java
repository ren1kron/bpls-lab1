package ifmo.se.lab1app.eis;

import ifmo.se.lab1app.shared.domain.Campaign;
import ifmo.se.lab1app.shared.domain.CampaignStatus;
import ifmo.se.lab1app.shared.domain.EisOutboxEvent;
import ifmo.se.lab1app.shared.infra.EisOutboxEventRepository;
import org.springframework.stereotype.Service;

@Service
public class CampaignEisOutboxService {

    private final EisOutboxEventRepository outboxRepository;
    private final CampaignEisEventPublisher eventPublisher;

    public CampaignEisOutboxService(
            EisOutboxEventRepository outboxRepository,
            CampaignEisEventPublisher eventPublisher
    ) {
        this.outboxRepository = outboxRepository;
        this.eventPublisher = eventPublisher;
    }

    public void enqueue(
            String eventKey,
            String eventType,
            CampaignStatus statusBefore,
            CampaignStatus statusAfter,
            Campaign campaign
    ) {
        enqueue(eventKey, eventType, statusBefore, statusAfter, null, campaign);
    }

    public void enqueue(
            String eventKey,
            String eventType,
            CampaignStatus statusBefore,
            CampaignStatus statusAfter,
            String comment,
            Campaign campaign
    ) {
        if (outboxRepository.existsByEventKey(eventKey)) {
            return;
        }
        String payload = eventPublisher.buildPayload(eventKey, eventType, statusBefore, statusAfter, comment, campaign);
        outboxRepository.save(EisOutboxEvent.unpublished(eventKey, eventType, payload));
    }
}
