package ifmo.se.lab1app.worker.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import ifmo.se.lab1app.client.domain.creative.Creative;
import ifmo.se.lab1app.client.infra.CreativeRepository;
import ifmo.se.lab1app.eis.CampaignEisEventPublisher;
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
    private final CampaignEisEventPublisher eisEventPublisher;

    public CreativeUploadSagaHandler(
            CreativeUploadTaskRepository taskRepository,
            CampaignRepository campaignRepository,
            CreativeRepository creativeRepository,
            KafkaOutboxEventRepository outboxRepository,
            TransactionExecutor transactions,
            ObjectMapper objectMapper,
            CreativeUploadKafkaProperties kafkaProperties,
            CampaignEisEventPublisher eisEventPublisher
    ) {
        this.taskRepository = taskRepository;
        this.campaignRepository = campaignRepository;
        this.creativeRepository = creativeRepository;
        this.outboxRepository = outboxRepository;
        this.transactions = transactions;
        this.objectMapper = objectMapper;
        this.kafkaProperties = kafkaProperties;
        this.eisEventPublisher = eisEventPublisher;
    }

    public void handleRequestPayload(String payload) {
        CreativeUploadRequestEvent event;
        try {
            event = objectMapper.readValue(payload, CreativeUploadRequestEvent.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Invalid creative upload request event payload", exception);
        }
        processRequest(event);
    }

    public void processRequest(CreativeUploadRequestEvent event) {
        try {
            transactions.write(() -> completeTask(event));
        } catch (RuntimeException exception) {
            log.error("Failed to process creative upload taskId={}", event.taskId(), exception);
            markTaskFailed(event.taskId(), exception.getMessage());
            throw exception;
        }
    }

    private void completeTask(CreativeUploadRequestEvent event) {
        CreativeUploadTask task = taskRepository.findByIdForUpdate(event.taskId())
                .orElseThrow(() -> new IllegalStateException("Creative upload task not found: " + event.taskId()));

        if (task.getStatus() == CreativeUploadStatus.COMPLETED || task.getStatus() == CreativeUploadStatus.FAILED) {
            return;
        }
        if (!task.getCampaignId().equals(event.campaignId())) {
            throw new IllegalStateException("Task campaign mismatch for taskId=" + event.taskId());
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
        outboxRepository.save(resultEvent(task));
        eisEventPublisher.publish("CreativeAdded", statusBefore, campaign.getStatus(), campaign);
    }

    private void markTaskFailed(String taskId, String error) {
        transactions.write(() -> {
            CreativeUploadTask task = taskRepository.findByIdForUpdate(taskId).orElse(null);
            if (task == null || task.getStatus() == CreativeUploadStatus.COMPLETED) {
                return;
            }

            task.setStatus(CreativeUploadStatus.FAILED);
            task.setError(error);
            campaignRepository.findByIdForUpdate(task.getCampaignId())
                    .ifPresent(this::refreshCampaignStatusAfterTaskFinished);
            outboxRepository.save(resultEvent(task));
        });
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
}
