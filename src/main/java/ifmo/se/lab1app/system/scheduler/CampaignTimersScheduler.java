package ifmo.se.lab1app.system.scheduler;

import ifmo.se.lab1app.system.application.SystemService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CampaignTimersScheduler {

    private final SystemService service;

    public CampaignTimersScheduler(SystemService service) {
        this.service = service;
    }

    public void processTimers() {
        log.info("Scheduler on campaigns processes...");
        service.processTimersForAllCampaigns();
        log.info("Scheduler on campaigns processed");
    }
}
