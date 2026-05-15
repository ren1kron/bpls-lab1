package ifmo.se.lab1app.camunda;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class CamundaRestClient {

    private static final Logger log = LoggerFactory.getLogger(CamundaRestClient.class);
    private static final DateTimeFormatter CAMUNDA_DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

    private final CamundaProperties properties;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String baseUrl;
    private final String authorizationHeader;

    public CamundaRestClient(CamundaProperties properties) {
        this.properties = properties;
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
        this.baseUrl = trimTrailingSlash(properties.getBaseUrl());
        if (StringUtils.hasText(properties.getUsername()) && StringUtils.hasText(properties.getPassword())) {
            String credentials = properties.getUsername() + ":" + properties.getPassword();
            this.authorizationHeader = "Basic " + Base64.getEncoder()
                    .encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
        } else {
            this.authorizationHeader = null;
        }
    }

    public List<CamundaExternalTask> fetchAndLock(Collection<String> topics) {
        if (topics == null || topics.isEmpty()) {
            return List.of();
        }
        Map<String, Object> body = Map.of(
                "workerId", properties.getWorkerId(),
                "maxTasks", properties.getMaxTasks(),
                "usePriority", true,
                "asyncResponseTimeout", properties.getAsyncResponseTimeoutMs(),
                "topics", topics.stream()
                        .map(topic -> Map.of(
                                "topicName", topic,
                                "lockDuration", properties.getLockDurationMs()
                        ))
                        .toList()
        );

        List<CamundaExternalTask> tasks = post(
                "/external-task/fetchAndLock",
                body,
                new TypeReference<>() {
                }
        );
        return tasks == null ? List.of() : tasks;
    }

    public void completeExternalTask(String taskId, Map<String, CamundaVariable> variables) {
        postNoResponse(
                "/external-task/" + taskId + "/complete",
                Map.of(
                        "workerId", properties.getWorkerId(),
                        "variables", variables == null ? Map.of() : variables
                )
        );
    }

    public void handleFailure(String taskId, String errorMessage, String errorDetails, Integer currentRetries) {
        int retries = currentRetries == null ? properties.getFailureRetries() : Math.max(currentRetries - 1, 0);
        postNoResponse(
                "/external-task/" + taskId + "/failure",
                Map.of(
                        "workerId", properties.getWorkerId(),
                        "errorMessage", truncate(errorMessage),
                        "errorDetails", errorDetails == null ? "" : errorDetails,
                        "retries", retries,
                        "retryTimeout", properties.getRetryTimeoutMs()
                )
        );
    }

    public String startProcess(String processDefinitionKey, String businessKey, Map<String, CamundaVariable> variables) {
        Map<String, Object> body = new LinkedHashMap<>();
        if (businessKey != null) {
            body.put("businessKey", businessKey);
        }
        body.put("variables", variables == null ? Map.of() : variables);
        ProcessStartResponse response = post(
                "/process-definition/key/" + processDefinitionKey + "/start",
                body,
                new TypeReference<>() {
                }
        );
        return response == null ? null : response.id();
    }

    public void completeUserTask(String taskId, Map<String, CamundaVariable> variables) {
        postNoResponse(
                "/task/" + taskId + "/complete",
                Map.of("variables", variables == null ? Map.of() : variables)
        );
    }

    public Optional<String> findTaskIdByCampaignAndDefinition(Long campaignId, String taskDefinitionKey) {
        Map<String, Object> body = Map.of(
                "active", true,
                "taskDefinitionKey", taskDefinitionKey,
                "processVariables", List.of(Map.of(
                        "name", "campaignId",
                        "operator", "eq",
                        "value", campaignId
                ))
        );
        List<TaskResponse> tasks = post(
                "/task",
                body,
                new TypeReference<>() {
                }
        );
        if (tasks == null || tasks.isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(tasks.get(0).id());
    }

    public void correlateMessage(
            String messageName,
            Map<String, CamundaVariable> correlationKeys,
            Map<String, CamundaVariable> processVariables
    ) {
        postNoResponse(
                "/message",
                Map.of(
                        "messageName", messageName,
                        "correlationKeys", correlationKeys == null ? Map.of() : correlationKeys,
                        "processVariables", processVariables == null ? Map.of() : processVariables,
                        "resultEnabled", true
                )
        );
    }

    public void correlateMessageBestEffort(
            String messageName,
            Map<String, CamundaVariable> correlationKeys,
            Map<String, CamundaVariable> processVariables
    ) {
        if (!properties.isEnabled()) {
            return;
        }
        try {
            correlateMessage(messageName, correlationKeys, processVariables);
        } catch (Exception exception) {
            log.warn("Failed to correlate Camunda message {}", messageName, exception);
        }
    }

    public void ensureGroup(String id, String name) {
        if (exists("/group/" + encodeResourceId(id))) {
            return;
        }
        try {
            postNoResponse("/group/create", Map.of(
                    "id", id,
                    "name", name,
                    "type", "WORKFLOW"
            ));
        } catch (CamundaRestException exception) {
            ignoreConflict(exception, "Camunda group already exists: " + id);
        }
    }

    public void ensureUser(String id, String password) {
        if (!StringUtils.hasText(password)) {
            return;
        }
        if (exists("/user/" + encodeResourceId(id) + "/profile")) {
            return;
        }
        try {
            postNoResponse("/user/create", Map.of(
                    "profile", Map.of(
                            "id", id,
                            "firstName", id,
                            "lastName", "",
                            "email", id + "@local"
                    ),
                    "credentials", Map.of("password", password)
            ));
        } catch (CamundaRestException exception) {
            ignoreConflict(exception, "Camunda user already exists: " + id);
        }
    }

    public void addUserToGroup(String userId, String groupId) {
        if (membershipExists(userId, groupId)) {
            return;
        }
        try {
            sendNoResponse(
                    "PUT",
                    "/group/" + encodeResourceId(groupId) + "/members/" + encodeResourceId(userId),
                    null
            );
        } catch (CamundaRestException exception) {
            ignoreConflict(exception, "Camunda membership already exists: " + userId + " -> " + groupId);
        }
    }

    private boolean membershipExists(String userId, String groupId) {
        List<Map<String, Object>> users = send(
                "GET",
                "/user?memberOfGroup=" + encodeQueryParam(groupId) + "&id=" + encodeQueryParam(userId),
                null,
                new TypeReference<>() {
                }
        );
        return users != null && !users.isEmpty();
    }

    private boolean exists(String path) {
        try {
            send(
                    "GET",
                    path,
                    null,
                    new TypeReference<Map<String, Object>>() {
                    }
            );
            return true;
        } catch (CamundaRestException exception) {
            if (exception.statusCode() == 404) {
                return false;
            }
            throw exception;
        }
    }

    private String encodeResourceId(String id) {
        return URLEncoder.encode(id, StandardCharsets.UTF_8);
    }

    private String encodeQueryParam(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    public static Map<String, CamundaVariable> variables(Map<String, ?> values) {
        if (values == null || values.isEmpty()) {
            return Map.of();
        }
        return values.entrySet().stream()
                .filter(entry -> entry.getValue() != null)
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> variable(entry.getValue())));
    }

    public static CamundaVariable dateVariable(LocalDateTime value) {
        if (value == null) {
            return CamundaVariable.date(null);
        }
        String formatted = value.atZone(ZoneId.systemDefault()).format(CAMUNDA_DATE_FORMAT);
        return CamundaVariable.date(formatted);
    }

    private static CamundaVariable variable(Object value) {
        if (value instanceof CamundaVariable variable) {
            return variable;
        }
        if (value instanceof Boolean bool) {
            return CamundaVariable.bool(bool);
        }
        if (value instanceof Number number) {
            return CamundaVariable.integer(number);
        }
        return CamundaVariable.string(Objects.toString(value, null));
    }

    private static String trimTrailingSlash(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private <T> T post(String path, Object body, TypeReference<T> responseType) {
        return send("POST", path, body, responseType);
    }

    private void postNoResponse(String path, Object body) {
        sendNoResponse("POST", path, body);
    }

    private void sendNoResponse(String method, String path, Object body) {
        send(method, path, body, null);
    }

    private <T> T send(String method, String path, Object body, TypeReference<T> responseType) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + path))
                    .header("Accept", "application/json");
            if (authorizationHeader != null) {
                builder.header("Authorization", authorizationHeader);
            }
            if (body == null) {
                builder.method(method, HttpRequest.BodyPublishers.noBody());
            } else {
                builder.header("Content-Type", "application/json; charset=utf-8")
                        .method(method, HttpRequest.BodyPublishers.ofString(toJson(body), StandardCharsets.UTF_8));
            }

            HttpResponse<String> response = httpClient.send(
                    builder.build(),
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
            );
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new CamundaRestException(response.statusCode(), response.body());
            }
            if (responseType == null || response.body() == null || response.body().isBlank()) {
                return null;
            }
            return objectMapper.readValue(response.body(), responseType);
        } catch (IOException exception) {
            throw new IllegalStateException("Camunda REST request failed: " + method + " " + path, exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Camunda REST request interrupted: " + method + " " + path, exception);
        }
    }

    private String toJson(Object body) {
        try {
            return objectMapper.writeValueAsString(body);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize Camunda REST payload", exception);
        }
    }

    private void ignoreConflict(CamundaRestException exception, String message) {
        if (exception.statusCode() == 409 || exception.isAlreadyExistsBadRequest()) {
            log.debug(message);
            return;
        }
        throw exception;
    }

    private String truncate(String value) {
        if (value == null) {
            return "";
        }
        return value.length() > 666 ? value.substring(0, 666) : value;
    }

    private record ProcessStartResponse(String id) {
    }

    private record TaskResponse(String id) {
    }

    private static class CamundaRestException extends RuntimeException {

        private final int statusCode;
        private final String body;

        private CamundaRestException(int statusCode, String body) {
            super("Camunda REST returned status=" + statusCode + " body=" + body);
            this.statusCode = statusCode;
            this.body = body == null ? "" : body;
        }

        private int statusCode() {
            return statusCode;
        }

        private boolean isAlreadyExistsBadRequest() {
            return statusCode == 400 && body.toLowerCase().contains("already exists");
        }
    }
}
