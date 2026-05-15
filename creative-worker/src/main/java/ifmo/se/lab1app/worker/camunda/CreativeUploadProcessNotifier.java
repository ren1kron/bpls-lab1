package ifmo.se.lab1app.worker.camunda;

import ifmo.se.lab1app.camunda.CamundaProperties;
import ifmo.se.lab1app.camunda.CamundaRestClient;
import ifmo.se.lab1app.camunda.CamundaVariable;
import ifmo.se.lab1app.shared.domain.CreativeUploadTask;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Component
public class CreativeUploadProcessNotifier {

    private final CamundaProperties properties;
    private final CamundaRestClient camunda;

    public CreativeUploadProcessNotifier(CamundaProperties properties, CamundaRestClient camunda) {
        this.properties = properties;
        this.camunda = camunda;
    }

    public void notifyFinished(CreativeUploadTask task) {
        if (!properties.isEnabled() || task == null || task.getCampaignId() == null) {
            return;
        }

        try {
            Map<String, CamundaVariable> variables = new LinkedHashMap<>();
            variables.put("creativeTaskId", CamundaVariable.string(task.getId()));
            variables.put("creativeUploadStatus", CamundaVariable.string(task.getStatus().name()));
            if (task.getError() != null) {
                variables.put("creativeUploadError", CamundaVariable.string(task.getError()));
            }
            camunda.correlateMessage(
                    "CreativeUploadFinished",
                    Map.of("campaignId", CamundaVariable.integer(task.getCampaignId())),
                    variables
            );
        } catch (Exception exception) {
            log.warn("Failed to notify Camunda about creative upload task={}", task.getId(), exception);
        }
    }
}
