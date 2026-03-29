package ifmo.se.lab1app.moderator.application;

import ifmo.se.lab1app.client.api.dto.CampaignResponse;
import ifmo.se.lab1app.billing.yookassa.application.YooKassaPaymentClient;
import ifmo.se.lab1app.billing.yookassa.application.YooKassaPaymentResult;
import ifmo.se.lab1app.exception.InvalidStateException;
import ifmo.se.lab1app.exception.NotFoundException;
import ifmo.se.lab1app.moderator.api.dto.ModerationDecisionRequest;
import ifmo.se.lab1app.shared.application.TransactionExecutor;
import ifmo.se.lab1app.shared.domain.Campaign;
import ifmo.se.lab1app.shared.domain.CampaignStatus;
import ifmo.se.lab1app.shared.infra.CampaignRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;

@Slf4j
@Service
public class ModeratorService {

    private final CampaignRepository campaignRepository;
    private final YooKassaPaymentClient yooKassaPaymentClient;
    private final TransactionExecutor transactions;

    public ModeratorService(
            CampaignRepository campaignRepository,
            YooKassaPaymentClient yooKassaPaymentClient,
            TransactionExecutor transactions
    ) {
        this.campaignRepository = campaignRepository;
        this.yooKassaPaymentClient = yooKassaPaymentClient;
        this.transactions = transactions;
    }

    public CampaignResponse processModerationDecision(Long campaignId, ModerationDecisionRequest request) {
        return transactions.write(() -> {
            Campaign campaign = findCampaign(campaignId);
            requireStatus(campaign, CampaignStatus.ON_MODERATION);

            campaign.setModerationComment(request.comment());

            if (Boolean.TRUE.equals(request.approved())) {
                YooKassaPaymentResult payment = yooKassaPaymentClient.createPayment(campaign);
                campaign.setPaymentId(payment.id());
                campaign.setPaymentConfirmationUrl(payment.confirmationUrl());
                campaign.setStatus(CampaignStatus.WAITING_PAYMENT);
            } else {
                campaign.setStatus(CampaignStatus.MODERATION_REJECTED);
                campaign.setPaymentId(null);
                campaign.setPaymentConfirmationUrl(null);
            }

            Campaign savedCampaign = campaignRepository.save(campaign);
            return CampaignResponse.from(savedCampaign);
        });
    }

    private Campaign findCampaign(Long campaignId) {
        return campaignRepository.findById(campaignId)
                .orElseThrow(() -> new NotFoundException("Campaign with id=" + campaignId + " was never found"));
    }

    private void requireStatus(Campaign campaign, CampaignStatus... allowedStatuses) {
        if (Arrays.stream(allowedStatuses).noneMatch(status -> status == campaign.getStatus())) {
            throw new InvalidStateException(
                    "Incorrect move from status " + campaign.getStatus() +
                            ". Allowed: " + Arrays.toString(allowedStatuses)
            );
        }
    }
}
