package ifmo.se.lab1app.system.scheduler;

import org.quartz.JobDetail;
import org.quartz.SimpleTrigger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.MethodInvokingJobDetailFactoryBean;
import org.springframework.scheduling.quartz.SimpleTriggerFactoryBean;

@Configuration
public class CampaignTimersQuartzConfig {

    private static final String CAMPAIGN_TIMERS_GROUP = "campaignTimers";

    @Bean
    public MethodInvokingJobDetailFactoryBean campaignTimersJobDetail(CampaignTimersScheduler scheduler) {
        MethodInvokingJobDetailFactoryBean factory = new MethodInvokingJobDetailFactoryBean();
        factory.setName("campaignTimersJob");
        factory.setGroup(CAMPAIGN_TIMERS_GROUP);
        factory.setTargetObject(scheduler);
        factory.setTargetMethod("processTimers");
        factory.setConcurrent(false);
        return factory;
    }

    @Bean
    public SimpleTriggerFactoryBean campaignTimersTrigger(
            JobDetail campaignTimersJobDetail,
            @Value("${app.timers.fixed-delay-ms:30000}") long fixedDelayMs
    ) {
        SimpleTriggerFactoryBean factory = new SimpleTriggerFactoryBean();
        factory.setName("campaignTimersTrigger");
        factory.setGroup(CAMPAIGN_TIMERS_GROUP);
        factory.setJobDetail(campaignTimersJobDetail);
        factory.setRepeatInterval(requirePositiveFixedDelay(fixedDelayMs));
        factory.setRepeatCount(SimpleTrigger.REPEAT_INDEFINITELY);
        factory.setMisfireInstruction(SimpleTrigger.MISFIRE_INSTRUCTION_RESCHEDULE_NEXT_WITH_EXISTING_COUNT);
        return factory;
    }

    private long requirePositiveFixedDelay(long fixedDelayMs) {
        if (fixedDelayMs <= 0) {
            throw new IllegalArgumentException("app.timers.fixed-delay-ms must be greater than zero");
        }
        return fixedDelayMs;
    }
}
