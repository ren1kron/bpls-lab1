package ifmo.se.lab1app.worker.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import ifmo.se.lab1app.client.domain.creative.Creative;
import ifmo.se.lab1app.client.infra.CreativeRepository;
import ifmo.se.lab1app.eis.CampaignEisOutboxService;
import ifmo.se.lab1app.shared.application.TransactionExecutor;
import ifmo.se.lab1app.shared.domain.Campaign;
import ifmo.se.lab1app.shared.domain.CampaignStatus;
import ifmo.se.lab1app.shared.domain.CreativeUploadStatus;
import ifmo.se.lab1app.shared.domain.CreativeUploadTask;
import ifmo.se.lab1app.shared.domain.KafkaOutboxEvent;
import ifmo.se.lab1app.shared.infra.CampaignRepository;
import ifmo.se.lab1app.shared.infra.CreativeUploadTaskRepository;
import ifmo.se.lab1app.shared.infra.KafkaOutboxEventRepository;
import ifmo.se.lab1app.shared.kafka.CreativeUploadKafkaProperties;
import ifmo.se.lab1app.shared.kafka.dto.CreativeUploadRequestEvent;
import ifmo.se.lab1app.shared.kafka.dto.CreativeUploadResultEvent;
import java.util.EnumSet;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class CreativeUploadSagaHandler {

    private static final Set<CreativeUploadStatus> ACTIVE_TASK_STATUSES = EnumSet.of(
            CreativeUploadStatus.PENDING,
            CreativeUploadStatus.PROCESSING
    );

    private final CreativeUploadTaskRepository taskRepository;
    private final CampaignRepository campaignRepository;
    private final CreativeRepository creativeRepository;
    private final KafkaOutboxEventRepository outboxRepository;
    private final TransactionExecutor transactions;
    private final ObjectMapper objectMapper;
    private final CreativeUploadKafkaProperties kafkaProperties;
    private final CampaignEisOutboxService eisOutboxService;

    public CreativeUploadSagaHandler(
            CreativeUploadTaskRepository taskRepository,
            CampaignRepository campaignRepository,
            CreativeRepository creativeRepository,
            KafkaOutboxEventRepository outboxRepository,
            TransactionExecutor transactions,
            ObjectMapper objectMapper,
            CreativeUploadKafkaProperties kafkaProperties,
            CampaignEisOutboxService eisOutboxService
    ) {
        this.taskRepository = taskRepository;
        this.campaignRepository = campaignRepository;
        this.creativeRepository = creativeRepository;
        this.outboxRepository = outboxRepository;
        this.transactions = transactions;
        this.objectMapper = objectMapper;
        this.kafkaProperties = kafkaProperties;
        this.eisOutboxService = eisOutboxService;
    }

    public void handleRequestPayload(String payload) {
        CreativeUploadRequestEvent event;
        try {
            event = objectMapper.readValue(payload, CreativeUploadRequestEvent.class);
        } catch (JsonProcessingException exception) {
            throw new MalformedCreativeUploadEventException("Invalid creative upload request event payload", exception);
        }
        processRequest(event);
    }

    public void processRequest(CreativeUploadRequestEvent event) {
        try {
            validateRequest(event);
            transactions.write(() -> completeTask(event));
        } catch (PermanentCreativeUploadFailureException exception) {
            log.warn("Permanent creative upload failure taskId={}: {}", taskId(event), exception.getMessage());
            markTaskFailed(taskId(event), exception.getMessage());
        } catch (RuntimeException exception) {
            log.error("Retryable creative upload failure taskId={}", taskId(event), exception);
            throw exception;
        }
    }

    private void completeTask(CreativeUploadRequestEvent event) {
        CreativeUploadTask task = taskRepository.findByIdForUpdate(event.taskId())
                .orElseThrow(() -> permanent("Creative upload task not found: " + event.taskId()));

        if (task.getStatus() == CreativeUploadStatus.COMPLETED || task.getStatus() == CreativeUploadStatus.FAILED) {
            return;
        }
        if (!task.getCampaignId().equals(event.campaignId())) {
            throw permanent("Task campaign mismatch for taskId=" + event.taskId());
        }
        if (!task.getUrl().equals(event.url()) || task.getType() != event.type()) {
            throw permanent("Task payload mismatch for taskId=" + event.taskId());
        }

        Campaign campaign = campaignRepository.findByIdForUpdate(task.getCampaignId())
                .orElseThrow(() -> new IllegalStateException("Campaign not found for taskId=" + event.taskId()));
        CampaignStatus statusBefore = campaign.getStatus();

        task.setStatus(CreativeUploadStatus.PROCESSING);
        Creative creative = creativeRepository.findByUploadTaskId(task.getId())
                .orElseGet(() -> {
                    Creative newCreative = new Creative();
                    newCreative.setCampaignId(task.getCampaignId());
                    newCreative.setName(task.getUrl());
                    newCreative.setType(task.getType());
                    newCreative.setUploadTaskId(task.getId());
                    return creativeRepository.save(newCreative);
                });

        task.setStatus(CreativeUploadStatus.COMPLETED);
        task.setCreativeId(creative.getId());
        task.setError(null);
        refreshCampaignStatusAfterTaskFinished(campaign);
        saveResultEventIfAbsent(task);
        eisOutboxService.enqueue(
                "creative-upload:" + task.getId() + ":creative-added",
                "CreativeAdded",
                statusBefore,
                campaign.getStatus(),
                campaign
        );
    }

    private void markTaskFailed(String taskId, String error) {
        if (taskId == null || taskId.isBlank()) {
            return;
        }
        transactions.write(() -> {
            CreativeUploadTask task = taskRepository.findByIdForUpdate(taskId).orElse(null);
            if (task == null
                    || task.getStatus() == CreativeUploadStatus.COMPLETED
                    || task.getStatus() == CreativeUploadStatus.FAILED) {
                return;
            }

            task.setStatus(CreativeUploadStatus.FAILED);
            task.setError(error);
            campaignRepository.findByIdForUpdate(task.getCampaignId())
                    .ifPresent(this::refreshCampaignStatusAfterTaskFinished);
            saveResultEventIfAbsent(task);
        });
    }

    private void saveResultEventIfAbsent(CreativeUploadTask task) {
        String topic = kafkaProperties.getUploadResultTopic();
        if (!outboxRepository.existsByTopicAndEventKey(topic, task.getId())) {
            outboxRepository.save(resultEvent(task));
        }
    }

    private void refreshCampaignStatusAfterTaskFinished(Campaign campaign) {
        long activeTasks = taskRepository.countByCampaignIdAndStatusIn(campaign.getId(), ACTIVE_TASK_STATUSES);
        if (activeTasks > 0) {
            campaign.setStatus(CampaignStatus.CREATIVES_LOADING);
            return;
        }

        if (creativeRepository.countByCampaignId(campaign.getId()) > 0) {
            campaign.setStatus(CampaignStatus.CREATIVES_UPLOADED);
        } else {
            campaign.setStatus(CampaignStatus.CONFIGURED);
        }
    }

    private KafkaOutboxEvent resultEvent(CreativeUploadTask task) {
        CreativeUploadResultEvent event = new CreativeUploadResultEvent(
                task.getId(),
                task.getCampaignId(),
                task.getStatus(),
                task.getCreativeId(),
                task.getError()
        );
        try {
            return KafkaOutboxEvent.unpublished(
                    kafkaProperties.getUploadResultTopic(),
                    task.getId(),
                    objectMapper.writeValueAsString(event)
            );
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize creative upload result event", exception);
        }
    }

    private void validateRequest(CreativeUploadRequestEvent event) {
        if (event == null) {
            throw permanent("Creative upload request event is null");
        }
        if (event.taskId() == null || event.taskId().isBlank()) {
            throw permanent("Creative upload request event taskId is blank");
        }
        if (event.campaignId() == null) {
            throw permanent("Creative upload request event campaignId is null");
        }
        if (event.url() == null || event.url().isBlank()) {
            throw permanent("Creative upload request event url is blank for taskId=" + event.taskId());
        }
        if (event.type() == null) {
            throw permanent("Creative upload request event type is null for taskId=" + event.taskId());
        }
    }

    private String taskId(CreativeUploadRequestEvent event) {
        return event == null ? null : event.taskId();
    }

    private PermanentCreativeUploadFailureException permanent(String message) {
        return new PermanentCreativeUploadFailureException(message);
    }

    private static class PermanentCreativeUploadFailureException extends RuntimeException {

        private PermanentCreativeUploadFailureException(String message) {
            super(message);
        }
    }
}
