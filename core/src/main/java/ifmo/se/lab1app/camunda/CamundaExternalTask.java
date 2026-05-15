package ifmo.se.lab1app.camunda;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Map;
import java.util.stream.Collectors;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CamundaExternalTask(
        String id,
        String topicName,
        Integer retries,
        Map<String, CamundaVariable> variables
) {

    public Map<String, Object> variableValues() {
        if (variables == null || variables.isEmpty()) {
            return Map.of();
        }
        return variables.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().value()));
    }
}
