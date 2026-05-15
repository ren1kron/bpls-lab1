package ifmo.se.lab1app.camunda;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CamundaExternalTaskDeserializationTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldIgnoreCamundaVariableValueInfo() throws Exception {
        String payload = """
                [{
                  "id": "task-1",
                  "activityId": "ServiceTask_CreateDraft",
                  "activityInstanceId": "ServiceTask_CreateDraft:instance-1",
                  "processInstanceId": "process-1",
                  "topicName": "campaign-create-draft",
                  "retries": null,
                  "variables": {
                    "campaignType": {
                      "type": "String",
                      "value": "DISPLAY",
                      "valueInfo": {}
                    }
                  }
                }]
                """;

        List<CamundaExternalTask> tasks = objectMapper.readValue(
                payload,
                new TypeReference<>() {
                }
        );

        assertThat(tasks).hasSize(1);
        assertThat(tasks.get(0).variableValues())
                .containsEntry("campaignType", "DISPLAY");
    }
}
