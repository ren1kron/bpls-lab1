package ifmo.se.lab1app.worker.kafka;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import ifmo.se.lab1app.client.domain.creative.Creative;
import ifmo.se.lab1app.client.domain.creative.CreativeType;
import ifmo.se.lab1app.client.infra.CreativeRepository;
import ifmo.se.lab1app.eis.CampaignEisOutboxService;
import ifmo.se.lab1app.shared.application.TransactionExecutor;
import ifmo.se.lab1app.shared.domain.Campaign;
import ifmo.se.lab1app.shared.domain.CampaignStatus;
import ifmo.se.lab1app.shared.domain.CreativeUploadStatus;
import ifmo.se.lab1app.shared.domain.CreativeUploadTask;
import ifmo.se.lab1app.shared.infra.CampaignRepository;
import ifmo.se.lab1app.shared.infra.CreativeUploadTaskRepository;
import ifmo.se.lab1app.shared.infra.KafkaOutboxEventRepository;
import ifmo.se.lab1app.shared.kafka.CreativeUploadKafkaProperties;
import ifmo.se.lab1app.shared.kafka.dto.CreativeUploadRequestEvent;
import ifmo.se.lab1app.worker.camunda.CreativeUploadProcessNotifier;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CreativeUploadSagaHandlerTest {

    @Mock
    private CreativeUploadTaskRepository taskRepository;

    @Mock
    private CampaignRepository campaignRepository;

    @Mock
    private CreativeRepository creativeRepository;

    @Mock
    private KafkaOutboxEventRepository outboxRepository;

    @Mock
    private TransactionExecutor transactions;

    @Mock
    private CampaignEisOutboxService eisOutboxService;

    @Mock
    private CreativeUploadProcessNotifier processNotifier;

    private CreativeUploadSagaHandler sagaHandler;
    private CreativeUploadKafkaProperties kafkaProperties;

    @BeforeEach
    void setUp() {
        doAnswer(invocation -> {
            invocation.<Runnable>getArgument(0).run();
            return null;
        }).when(transactions).write(any(Runnable.class));

        kafkaProperties = new CreativeUploadKafkaProperties();
        sagaHandler = new CreativeUploadSagaHandler(
                taskRepository,
                campaignRepository,
                creativeRepository,
                outboxRepository,
                transactions,
                new ObjectMapper(),
                kafkaProperties,
                eisOutboxService,
                processNotifier
        );
    }

    @Test
    void shouldCompleteOnRetryAfterRetryableFailureWithoutCreatingDuplicateCreative() {
        CreativeUploadTask task = task("https://cdn.example.com/retry.png");
        Campaign campaign = new Campaign();
        campaign.setId(task.getCampaignId());
        campaign.setStatus(CampaignStatus.CREATIVES_LOADING);
        CreativeUploadRequestEvent request = request(task);

        when(taskRepository.findByIdForUpdate(task.getId())).thenReturn(Optional.of(task));
        when(campaignRepository.findByIdForUpdate(task.getCampaignId()))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(campaign));
        when(creativeRepository.findByUploadTaskId(task.getId())).thenReturn(Optional.empty());
        when(creativeRepository.save(any(Creative.class))).thenAnswer(invocation -> {
            Creative creative = invocation.getArgument(0);
            creative.setId(101L);
            return creative;
        });
        when(taskRepository.countByCampaignIdAndStatusIn(eq(task.getCampaignId()), any())).thenReturn(0L);
        when(creativeRepository.countByCampaignId(task.getCampaignId())).thenReturn(1L);
        when(outboxRepository.existsByTopicAndEventKey(kafkaProperties.getUploadResultTopic(), task.getId()))
                .thenReturn(false);

        assertThatThrownBy(() -> sagaHandler.processRequest(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Campaign not found");
        assertThat(task.getStatus()).isEqualTo(CreativeUploadStatus.PENDING);
        verifyNoInteractions(eisOutboxService);

        sagaHandler.processRequest(request);

        assertThat(task.getStatus()).isEqualTo(CreativeUploadStatus.COMPLETED);
        assertThat(task.getCreativeId()).isEqualTo(101L);
        assertThat(campaign.getStatus()).isEqualTo(CampaignStatus.CREATIVES_UPLOADED);
        verify(creativeRepository).save(any(Creative.class));
        verify(outboxRepository).save(any());
        verify(eisOutboxService).enqueue(
                "creative-upload:" + task.getId() + ":creative-added",
                "CreativeAdded",
                CampaignStatus.CREATIVES_LOADING,
                CampaignStatus.CREATIVES_UPLOADED,
                campaign
        );
        verify(processNotifier).notifyFinished(task);
    }

    @Test
    void shouldRecordPermanentFailureWithoutThrowing() {
        CreativeUploadTask task = task("https://cdn.example.com/mismatch.png");
        Campaign campaign = new Campaign();
        campaign.setId(task.getCampaignId());
        campaign.setStatus(CampaignStatus.CREATIVES_LOADING);

        when(taskRepository.findByIdForUpdate(task.getId())).thenReturn(Optional.of(task));
        when(campaignRepository.findByIdForUpdate(task.getCampaignId())).thenReturn(Optional.of(campaign));
        when(outboxRepository.existsByTopicAndEventKey(kafkaProperties.getUploadResultTopic(), task.getId()))
                .thenReturn(false);

        sagaHandler.processRequest(new CreativeUploadRequestEvent(
                task.getId(),
                task.getCampaignId() + 1,
                task.getUrl(),
                task.getType()
        ));

        assertThat(task.getStatus()).isEqualTo(CreativeUploadStatus.FAILED);
        assertThat(task.getError()).contains("Task campaign mismatch");
        verify(outboxRepository).save(any());
        verifyNoInteractions(eisOutboxService);
        verify(processNotifier).notifyFinished(task);
    }

    private CreativeUploadTask task(String url) {
        CreativeUploadTask task = new CreativeUploadTask();
        task.setId(UUID.randomUUID().toString());
        task.setCampaignId(42L);
        task.setUrl(url);
        task.setType(CreativeType.IMAGE);
        task.setStatus(CreativeUploadStatus.PENDING);
        return task;
    }

    private CreativeUploadRequestEvent request(CreativeUploadTask task) {
        return new CreativeUploadRequestEvent(
                task.getId(),
                task.getCampaignId(),
                task.getUrl(),
                task.getType()
        );
    }
}
