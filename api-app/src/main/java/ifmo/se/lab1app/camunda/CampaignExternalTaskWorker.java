package ifmo.se.lab1app.camunda;

import ifmo.se.lab1app.shared.domain.Campaign;
import ifmo.se.lab1app.shared.domain.CreativeUploadTask;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CampaignExternalTaskWorker implements SmartLifecycle {

    private static final List<String> TOPICS = List.of(
            "campaign-create-draft",
            "campaign-update-basics",
            "campaign-configure",
            "campaign-reconfigure",
            "creative-schedule-upload",
            "creative-delete",
            "campaign-submit",
            "moderation-decision",
            "payment-succeeded",
            "payment-canceled",
            "payment-retry-invoice",
            "campaign-activate",
            "campaign-freeze",
            "campaign-resume",
            "campaign-stop",
            "campaign-delete"
    );

    private final CamundaProperties properties;
    private final CamundaRestClient camunda;
    private final CampaignProcessCommandService commands;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private Thread thread;

    public CampaignExternalTaskWorker(
            CamundaProperties properties,
            CamundaRestClient camunda,
            CampaignProcessCommandService commands
    ) {
        this.properties = properties;
        this.camunda = camunda;
        this.commands = commands;
    }

    @Override
    public void start() {
        if (!properties.isEnabled() || !properties.isExternalTaskWorkerEnabled()) {
            return;
        }
        if (!running.compareAndSet(false, true)) {
            return;
        }
        thread = new Thread(this::pollLoop, "camunda-campaign-external-task-worker");
        thread.start();
    }

    @Override
    public void stop() {
        running.set(false);
        if (thread != null) {
            thread.interrupt();
        }
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }

    private void pollLoop() {
        while (running.get()) {
            try {
                for (CamundaExternalTask task : camunda.fetchAndLock(TOPICS)) {
                    process(task);
                }
            } catch (Exception exception) {
                log.warn("Camunda external task polling failed", exception);
                sleepQuietly(properties.getRetryTimeoutMs());
            }
        }
    }

    private void process(CamundaExternalTask task) {
        try {
            Map<String, Object> variables = task.variableValues();
            Map<String, CamundaVariable> completionVariables = switch (task.topicName()) {
                case "campaign-create-draft" -> createDraft(variables);
                case "campaign-update-basics" -> campaignVariables(commands.updateBasics(campaignId(variables), variables));
                case "campaign-configure" -> campaignVariables(commands.configure(campaignId(variables), variables));
                case "campaign-reconfigure" -> campaignVariables(commands.reconfigure(campaignId(variables), variables));
                case "creative-schedule-upload" -> creativeUploadVariables(commands.scheduleCreativeUpload(campaignId(variables), variables));
                case "creative-delete" -> campaignVariables(commands.deleteCreative(campaignId(variables), variables));
                case "campaign-submit" -> campaignVariables(commands.submitForModeration(campaignId(variables), variables));
                case "moderation-decision" -> moderationVariables(commands.processModerationDecision(campaignId(variables), variables), variables);
                case "payment-succeeded" -> paymentSucceededVariables(commands.markPaymentSucceeded(campaignId(variables)));
                case "payment-canceled" -> campaignVariables(commands.markPaymentCanceled(campaignId(variables)));
                case "payment-retry-invoice" -> invoiceVariables(commands.retryPayment(campaignId(variables), variables));
                case "campaign-activate" -> activationVariables(commands.activate(campaignId(variables)));
                case "campaign-freeze" -> campaignVariables(commands.freezeActive(campaignId(variables), variables));
                case "campaign-resume" -> campaignVariables(commands.resume(campaignId(variables), variables));
                case "campaign-stop" -> campaignVariables(commands.stop(campaignId(variables), variables));
                case "campaign-delete" -> {
                    commands.deleteCampaign(campaignId(variables), variables);
                    yield Map.of();
                }
                default -> throw new IllegalStateException("Unsupported Camunda topic: " + task.topicName());
            };
            camunda.completeExternalTask(task.id(), completionVariables);
            log.info("Completed Camunda external task id={} topic={}", task.id(), task.topicName());
        } catch (Exception exception) {
            log.warn("Camunda external task failed id={} topic={}", task.id(), task.topicName(), exception);
            camunda.handleFailure(task.id(), exception.getMessage(), stackTrace(exception), task.retries());
        }
    }

    private Map<String, CamundaVariable> createDraft(Map<String, Object> variables) {
        Campaign campaign = commands.createDraft(variables);
        return campaignVariables(campaign);
    }

    private Map<String, CamundaVariable> moderationVariables(Campaign campaign, Map<String, Object> variables) {
        Map<String, CamundaVariable> result = new LinkedHashMap<>(campaignVariables(campaign));
        result.put("moderationApproved", CamundaVariable.bool(booleanValue(variables, "moderationApproved")));
        if (campaign.getPaymentId() != null) {
            result.put("paymentId", CamundaVariable.string(campaign.getPaymentId()));
        }
        if (campaign.getPaymentConfirmationUrl() != null) {
            result.put("paymentConfirmationUrl", CamundaVariable.string(campaign.getPaymentConfirmationUrl()));
        }
        return result;
    }

    private Map<String, CamundaVariable> paymentSucceededVariables(Campaign campaign) {
        Map<String, CamundaVariable> result = new LinkedHashMap<>(campaignVariables(campaign));
        result.put("startAt", CamundaRestClient.dateVariable(campaign.getRequestedStartAt()));
        return result;
    }

    private Map<String, CamundaVariable> invoiceVariables(Campaign campaign) {
        Map<String, CamundaVariable> result = new LinkedHashMap<>(campaignVariables(campaign));
        if (campaign.getPaymentId() != null) {
            result.put("paymentId", CamundaVariable.string(campaign.getPaymentId()));
        }
        if (campaign.getPaymentConfirmationUrl() != null) {
            result.put("paymentConfirmationUrl", CamundaVariable.string(campaign.getPaymentConfirmationUrl()));
        }
        return result;
    }

    private Map<String, CamundaVariable> activationVariables(Campaign campaign) {
        Map<String, CamundaVariable> result = new LinkedHashMap<>(campaignVariables(campaign));
        result.put("endAt", CamundaRestClient.dateVariable(campaign.getActualEndAt()));
        return result;
    }

    private Map<String, CamundaVariable> creativeUploadVariables(CreativeUploadTask task) {
        return Map.of(
                "campaignId", CamundaVariable.integer(task.getCampaignId()),
                "creativeTaskId", CamundaVariable.string(task.getId()),
                "creativeUploadStatus", CamundaVariable.string(task.getStatus().name())
        );
    }

    private Map<String, CamundaVariable> campaignVariables(Campaign campaign) {
        Map<String, CamundaVariable> result = new LinkedHashMap<>();
        result.put("campaignId", CamundaVariable.integer(campaign.getId()));
        result.put("ownerUsername", CamundaVariable.string(campaign.getOwner().getUsername()));
        result.put("campaignStatus", CamundaVariable.string(campaign.getStatus().name()));
        return result;
    }

    private Long campaignId(Map<String, Object> variables) {
        Object value = variables.get("campaignId");
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value == null) {
            throw new IllegalStateException("campaignId process variable is required");
        }
        return Long.parseLong(value.toString());
    }

    private boolean booleanValue(Map<String, Object> variables, String name) {
        Object value = variables.get(name);
        if (value instanceof Boolean bool) {
            return bool;
        }
        return Boolean.parseBoolean(value == null ? "false" : value.toString());
    }

    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(Math.max(1, millis));
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            running.set(false);
        }
    }

    private String stackTrace(Exception exception) {
        StringWriter writer = new StringWriter();
        exception.printStackTrace(new PrintWriter(writer));
        return writer.toString();
    }
}
