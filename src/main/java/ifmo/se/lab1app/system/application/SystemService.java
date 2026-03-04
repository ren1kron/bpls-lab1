package ifmo.se.lab1app.system.application;

import ifmo.se.lab1app.billing.yookassa.dto.PaymentObject;
import ifmo.se.lab1app.billing.yookassa.dto.YooKassaNotification;
import ifmo.se.lab1app.shared.domain.Campaign;
import ifmo.se.lab1app.shared.domain.CampaignStatus;
import ifmo.se.lab1app.shared.infra.CampaignRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@Transactional
public class SystemService {

    private static final Set<CampaignStatus> statuses = EnumSet.of(
            CampaignStatus.WAITING_START,
            CampaignStatus.ACTIVE
    );


    private final CampaignRepository campaignRepository;

    public SystemService(CampaignRepository campaignRepository) {
        this.campaignRepository = campaignRepository;
    }

    public void processPayment(YooKassaNotification notification) {
        PaymentObject payment = notification.object();

        Campaign campaign = campaignRepository.findByPaymentId(payment.id());
        switch (notification.event()) {
            case "payment.succeeded" -> {
                campaign.setStatus(CampaignStatus.WAITING_START);
            }
            case "payment.canceled" -> {
                campaign.setStatus(CampaignStatus.FROZEN_NO_PAYMENT);
            }
            default -> {
                log.warn("Unsupported event: {}", notification.event());
            }
        }
    }

    public void processTimersForAllCampaigns() {
        List<Campaign> campaigns = campaignRepository.findByStatusIn(statuses);
        LocalDateTime now = LocalDateTime.now();
        for (Campaign campaign : campaigns) {
            processTimers(campaign, now);
        }
        campaignRepository.saveAll(campaigns);
    }

    private void processTimers(Campaign campaign, LocalDateTime now) {
        if (campaign.getStatus() == CampaignStatus.WAITING_START && !campaign.getStartAt().isAfter(now)) {
            campaign.setStatus(CampaignStatus.ACTIVE);
        }

        if (campaign.getStatus() == CampaignStatus.ACTIVE && !campaign.getEndAt().isAfter(now)) {
            campaign.setStatus(CampaignStatus.STOPPED);
        }
    }
}
