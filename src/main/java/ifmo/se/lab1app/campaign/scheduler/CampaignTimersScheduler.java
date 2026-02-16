package ifmo.se.lab1app.campaign.scheduler;

import ifmo.se.lab1app.campaign.service.CampaignWorkflowService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class CampaignTimersScheduler {

    private final CampaignWorkflowService campaignWorkflowService;

    public CampaignTimersScheduler(CampaignWorkflowService campaignWorkflowService) {
        this.campaignWorkflowService = campaignWorkflowService;
    }

    @Scheduled(fixedDelayString = "${app.timers.fixed-delay-ms:30000}")
    public void processTimers() {
        campaignWorkflowService.processTimersForAllCampaigns();
    }
}
