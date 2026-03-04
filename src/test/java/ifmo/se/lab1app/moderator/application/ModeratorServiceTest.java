package ifmo.se.lab1app.moderator.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ifmo.se.lab1app.billing.yookassa.application.YooKassaPaymentClient;
import ifmo.se.lab1app.billing.yookassa.application.YooKassaPaymentResult;
import ifmo.se.lab1app.moderator.api.dto.ModerationDecisionRequest;
import ifmo.se.lab1app.shared.domain.Campaign;
import ifmo.se.lab1app.shared.domain.CampaignStatus;
import ifmo.se.lab1app.shared.infra.CampaignRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ModeratorServiceTest {

    @Mock
    private CampaignRepository campaignRepository;

    @Mock
    private YooKassaPaymentClient yooKassaPaymentClient;

    @InjectMocks
    private ModeratorService moderatorService;

    @Test
    void shouldPersistPaymentUrlWhenModerationApproved() {
        Campaign campaign = new Campaign();
        campaign.setId(42L);
        campaign.setName("Spring campaign");
        campaign.setStatus(CampaignStatus.ON_MODERATION);

        when(campaignRepository.findById(42L)).thenReturn(Optional.of(campaign));
        when(yooKassaPaymentClient.createPayment(campaign))
            .thenReturn(new YooKassaPaymentResult("payment-1", "pending", "https://pay.example/confirm"));
        when(campaignRepository.save(any(Campaign.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = moderatorService.processModerationDecision(
            42L,
            new ModerationDecisionRequest(true, "approved")
        );

        assertThat(campaign.getPaymentId()).isEqualTo("payment-1");
        assertThat(campaign.getPaymentUrl()).isEqualTo("https://pay.example/confirm");
        assertThat(campaign.getStatus()).isEqualTo(CampaignStatus.WAITING_PAYMENT);
        assertThat(response.paymentUrl()).isEqualTo("https://pay.example/confirm");
        assertThat(response.paymentConfirmationUrl()).isEqualTo("https://pay.example/confirm");
        verify(campaignRepository).save(campaign);
    }
}
