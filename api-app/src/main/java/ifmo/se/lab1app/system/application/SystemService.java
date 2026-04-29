package ifmo.se.lab1app.system.application;

import ifmo.se.lab1app.billing.yookassa.dto.PaymentObject;
import ifmo.se.lab1app.billing.yookassa.dto.YooKassaNotification;
import ifmo.se.lab1app.eis.CampaignEisEventPublisher;
import ifmo.se.lab1app.shared.application.TransactionExecutor;
import ifmo.se.lab1app.shared.domain.Campaign;
import ifmo.se.lab1app.shared.domain.CampaignStatus;
import ifmo.se.lab1app.shared.infra.CampaignRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
public class SystemService {

    private static final Set<CampaignStatus> statuses = EnumSet.of(
            CampaignStatus.WAITING_START,
            CampaignStatus.ACTIVE
    );


    private final CampaignRepository campaignRepository;
    private final TransactionExecutor transactions;
    private final CampaignEisEventPublisher eisEventPublisher;

    public SystemService(
            CampaignRepository campaignRepository,
            TransactionExecutor transactions,
            CampaignEisEventPublisher eisEventPublisher
    ) {
        this.campaignRepository = campaignRepository;
        this.transactions = transactions;
        this.eisEventPublisher = eisEventPublisher;
    }

    public void processPayment(YooKassaNotification notification) {
        transactions.write(() -> {
            PaymentObject payment = notification.object();

            Campaign campaign = campaignRepository.findByPaymentId(payment.id());
            CampaignStatus statusBefore = campaign.getStatus();
            String eventType = null;
            switch (notification.event()) {
                case "payment.succeeded" -> {
                    campaign.setStatus(CampaignStatus.WAITING_START);
                    eventType = "PaymentSucceeded";
                }
                case "payment.canceled" -> {
                    campaign.setStatus(CampaignStatus.FROZEN);
                    eventType = "PaymentCanceled";
                }
                default -> log.warn("Unsupported event: {}", notification.event());
            }

            Campaign savedCampaign = campaignRepository.save(campaign);
            if (eventType != null) {
                eisEventPublisher.publish(eventType, statusBefore, savedCampaign.getStatus(), notification.event(), savedCampaign);
            }
        });
    }

    public void processTimersForAllCampaigns() {
        transactions.write(() -> {
            List<Campaign> campaigns = campaignRepository.findByStatusIn(statuses);
            LocalDateTime now = LocalDateTime.now();
            for (Campaign campaign : campaigns) {
                processTimers(campaign, now);
            }
            campaignRepository.saveAll(campaigns);
        });
    }

    private void processTimers(Campaign campaign, LocalDateTime now) {
        if (campaign.getStatus() == CampaignStatus.WAITING_START && !campaign.getRequestedStartAt().isAfter(now)) {
            CampaignStatus statusBefore = campaign.getStatus();
            campaign.setActualStartAt(now);
            campaign.setActualEndAt(now.plusDays(campaign.getDurationDays()));
            campaign.setStatus(CampaignStatus.ACTIVE);
            eisEventPublisher.publish("CampaignStarted", statusBefore, campaign.getStatus(), campaign);
        }

        if (campaign.getStatus() == CampaignStatus.ACTIVE && !campaign.getActualEndAt().isAfter(now)) {
            CampaignStatus statusBefore = campaign.getStatus();
            campaign.setStatus(CampaignStatus.STOPPED);
            eisEventPublisher.publish("CampaignStopped", statusBefore, campaign.getStatus(), campaign);
        }
    }
}
