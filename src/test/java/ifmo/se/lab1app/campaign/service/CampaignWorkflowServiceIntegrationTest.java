package ifmo.se.lab1app.campaign.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ifmo.se.lab1app.campaign.dto.ConfigureCampaignRequest;
import ifmo.se.lab1app.campaign.dto.CreateCampaignRequest;
import ifmo.se.lab1app.campaign.dto.ModerationDecisionRequest;
import ifmo.se.lab1app.campaign.dto.PaymentReceivedRequest;
import ifmo.se.lab1app.campaign.dto.ResumeDecisionRequest;
import ifmo.se.lab1app.campaign.dto.UploadCreativesRequest;
import ifmo.se.lab1app.campaign.dto.ValidationResultRequest;
import ifmo.se.lab1app.campaign.exception.InvalidStateException;
import ifmo.se.lab1app.campaign.model.CampaignStatus;
import ifmo.se.lab1app.campaign.repository.CampaignRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class CampaignWorkflowServiceIntegrationTest {

    @Autowired
    private CampaignWorkflowService campaignWorkflowService;

    @Autowired
    private CampaignRepository campaignRepository;

    @BeforeEach
    void cleanDatabase() {
        campaignRepository.deleteAll();
    }

    @Test
    void shouldPassMainBpmnFlowUntilStop() {
        LocalDateTime now = LocalDateTime.now();
        var created = campaignWorkflowService.createCampaign(
            new CreateCampaignRequest(
                "Summer Campaign",
                "adv-1",
                new BigDecimal("10000.00"),
                now.minusHours(1),
                now.plusDays(3),
                3
            )
        );

        var configured = campaignWorkflowService.configureCampaign(created.id(), new ConfigureCampaignRequest("{\"geo\":\"RU\"}"));
        assertThat(configured.status()).isEqualTo(CampaignStatus.CONFIGURED);

        var withCreatives = campaignWorkflowService.uploadCreatives(created.id(), new UploadCreativesRequest("[\"banner-1.png\"]"));
        assertThat(withCreatives.status()).isEqualTo(CampaignStatus.CREATIVES_UPLOADED);

        campaignWorkflowService.submitForCheck(created.id());
        campaignWorkflowService.processValidationResult(created.id(), new ValidationResultRequest(true, "ok"));

        var waitingPayment = campaignWorkflowService.processModerationDecision(
            created.id(),
            new ModerationDecisionRequest(true, "approved")
        );

        assertThat(waitingPayment.status()).isEqualTo(CampaignStatus.WAITING_PAYMENT);
        assertThat(waitingPayment.invoices()).hasSize(1);

        var active = campaignWorkflowService.markPaymentReceived(created.id(), new PaymentReceivedRequest(now));
        assertThat(active.status()).isEqualTo(CampaignStatus.ACTIVE);

        var paused = campaignWorkflowService.requestPause(created.id());
        assertThat(paused.status()).isEqualTo(CampaignStatus.PAUSED);

        var resumed = campaignWorkflowService.decideResumeOrStop(created.id(), new ResumeDecisionRequest(true, true));
        assertThat(resumed.status()).isEqualTo(CampaignStatus.ACTIVE);

        var stopped = campaignWorkflowService.markBudgetExhausted(created.id());
        assertThat(stopped.status()).isEqualTo(CampaignStatus.STOPPED);
    }

    @Test
    void shouldRejectInvalidTransition() {
        LocalDateTime now = LocalDateTime.now();
        var created = campaignWorkflowService.createCampaign(
            new CreateCampaignRequest(
                "Bad Transition Campaign",
                "adv-2",
                new BigDecimal("5000.00"),
                now.plusHours(2),
                now.plusDays(2),
                2
            )
        );

        assertThatThrownBy(() -> campaignWorkflowService.submitForCheck(created.id()))
            .isInstanceOf(InvalidStateException.class)
            .hasMessageContaining("Некорректный переход");
    }
}
