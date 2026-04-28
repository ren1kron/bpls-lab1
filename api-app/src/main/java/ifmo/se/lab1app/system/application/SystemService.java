package ifmo.se.lab1app.system.application;

import ifmo.se.lab1app.billing.yookassa.dto.PaymentObject;
import ifmo.se.lab1app.billing.yookassa.dto.YooKassaNotification;
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

    public SystemService(CampaignRepository campaignRepository, TransactionExecutor transactions) {
        this.campaignRepository = campaignRepository;
        this.transactions = transactions;
    }

    public void processPayment(YooKassaNotification notification) {
        transactions.write(() -> {
            PaymentObject payment = notification.object();

            Campaign campaign = campaignRepository.findByPaymentId(payment.id());
            switch (notification.event()) {
                case "payment.succeeded" -> campaign.setStatus(CampaignStatus.WAITING_START);
                case "payment.canceled" -> campaign.setStatus(CampaignStatus.FROZEN);
                default -> log.warn("Unsupported event: {}", notification.event());
            }

            campaignRepository.save(campaign);
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
            campaign.setActualStartAt(now);
            campaign.setActualEndAt(now.plusDays(campaign.getDurationDays()));
            campaign.setStatus(CampaignStatus.ACTIVE);
        }

        if (campaign.getStatus() == CampaignStatus.ACTIVE && !campaign.getActualEndAt().isAfter(now)) {
            campaign.setStatus(CampaignStatus.STOPPED);
        }
    }
}
