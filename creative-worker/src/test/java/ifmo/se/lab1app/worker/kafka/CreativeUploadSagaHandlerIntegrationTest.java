package ifmo.se.lab1app.worker.kafka;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ifmo.se.lab1app.auth.domain.UserAccount;
import ifmo.se.lab1app.auth.infra.UserAccountRepository;
import ifmo.se.lab1app.client.domain.creative.CreativeType;
import ifmo.se.lab1app.client.domain.enums.CampaignObjective;
import ifmo.se.lab1app.client.domain.enums.CampaignType;
import ifmo.se.lab1app.client.domain.enums.StartMode;
import ifmo.se.lab1app.client.infra.CreativeRepository;
import ifmo.se.lab1app.shared.domain.Campaign;
import ifmo.se.lab1app.shared.domain.CampaignStatus;
import ifmo.se.lab1app.shared.domain.CreativeUploadStatus;
import ifmo.se.lab1app.shared.domain.CreativeUploadTask;
import ifmo.se.lab1app.shared.domain.UserRole;
import ifmo.se.lab1app.shared.infra.CampaignRepository;
import ifmo.se.lab1app.shared.infra.CreativeUploadTaskRepository;
import ifmo.se.lab1app.shared.infra.KafkaOutboxEventRepository;
import ifmo.se.lab1app.shared.kafka.dto.CreativeUploadRequestEvent;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

@SpringBootTest
class CreativeUploadSagaHandlerIntegrationTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private CreativeUploadSagaHandler sagaHandler;

    @Autowired
    private CampaignRepository campaignRepository;

    @Autowired
    private CreativeRepository creativeRepository;

    @Autowired
    private CreativeUploadTaskRepository taskRepository;

    @Autowired
    private KafkaOutboxEventRepository outboxEventRepository;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @BeforeEach
    void cleanDatabase() {
        outboxEventRepository.deleteAll();
        taskRepository.deleteAll();
        creativeRepository.deleteAll();
        campaignRepository.deleteAll();
        userAccountRepository.deleteAll();
    }

    @Test
    void shouldStartWorkerContextWithoutKafkaConsumerWhenDisabled() {
        assertThat(applicationContext.containsBean("creativeUploadKafkaWorker")).isFalse();
    }

    @Test
    void shouldCompleteCreativeUploadTask() {
        Campaign campaign = createCampaign();
        CreativeUploadTask task = createTask(campaign, "https://cdn.example.com/worker.png");

        sagaHandler.processRequest(new CreativeUploadRequestEvent(
                task.getId(),
                campaign.getId(),
                task.getUrl(),
                task.getType()
        ));

        CreativeUploadTask completedTask = taskRepository.findById(task.getId()).orElseThrow();
        assertThat(completedTask.getStatus()).isEqualTo(CreativeUploadStatus.COMPLETED);
        assertThat(completedTask.getCreativeId()).isNotNull();
        assertThat(creativeRepository.countByCampaignId(campaign.getId())).isEqualTo(1);
        assertThat(campaignRepository.findById(campaign.getId()).orElseThrow().getStatus())
                .isEqualTo(CampaignStatus.CREATIVES_UPLOADED);
        assertThat(outboxEventRepository.count()).isEqualTo(1);
    }

    @Test
    void shouldKeepCreativeUploadIdempotentWhenSameTaskIsConsumedTwice() {
        Campaign campaign = createCampaign();
        CreativeUploadTask task = createTask(campaign, "https://cdn.example.com/idempotent.png");
        CreativeUploadRequestEvent event = new CreativeUploadRequestEvent(
                task.getId(),
                campaign.getId(),
                task.getUrl(),
                task.getType()
        );

        sagaHandler.processRequest(event);
        sagaHandler.processRequest(event);

        assertThat(creativeRepository.countByCampaignId(campaign.getId())).isEqualTo(1);
        assertThat(taskRepository.findById(task.getId()).orElseThrow().getStatus())
                .isEqualTo(CreativeUploadStatus.COMPLETED);
    }

    @Test
    void shouldMarkTaskFailedWhenEventDoesNotMatchTaskCampaign() {
        Campaign campaign = createCampaign();
        CreativeUploadTask task = createTask(campaign, "https://cdn.example.com/failure.png");

        assertThatThrownBy(() -> sagaHandler.processRequest(new CreativeUploadRequestEvent(
                task.getId(),
                campaign.getId() + 100,
                task.getUrl(),
                task.getType()
        ))).isInstanceOf(IllegalStateException.class);

        CreativeUploadTask failedTask = taskRepository.findById(task.getId()).orElseThrow();
        assertThat(failedTask.getStatus()).isEqualTo(CreativeUploadStatus.FAILED);
        assertThat(failedTask.getError()).contains("Task campaign mismatch");
        assertThat(creativeRepository.countByCampaignId(campaign.getId())).isZero();
        assertThat(campaignRepository.findById(campaign.getId()).orElseThrow().getStatus())
                .isEqualTo(CampaignStatus.CONFIGURED);
        assertThat(outboxEventRepository.count()).isEqualTo(1);
    }

    private Campaign createCampaign() {
        UserAccount owner = new UserAccount();
        owner.setUsername("worker-client-" + UUID.randomUUID());
        owner.setPassword("{noop}secret123");
        owner.setRole(UserRole.CLIENT);
        UserAccount savedOwner = userAccountRepository.save(owner);

        Campaign campaign = new Campaign();
        campaign.setName("Worker campaign");
        campaign.setObjective(CampaignObjective.TRAFFIC);
        campaign.setType(CampaignType.DISPLAY);
        campaign.setStartMode(StartMode.MANUAL_START);
        campaign.setUrl("https://example.com");
        campaign.setStatus(CampaignStatus.CREATIVES_LOADING);
        campaign.setOwner(savedOwner);
        return campaignRepository.save(campaign);
    }

    private CreativeUploadTask createTask(Campaign campaign, String url) {
        CreativeUploadTask task = new CreativeUploadTask();
        task.setId(UUID.randomUUID().toString());
        task.setCampaignId(campaign.getId());
        task.setUrl(url);
        task.setType(CreativeType.IMAGE);
        task.setStatus(CreativeUploadStatus.PENDING);
        return taskRepository.save(task);
    }
}
