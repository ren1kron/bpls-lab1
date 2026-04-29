package ifmo.se.lab1app.auth.application;

import ifmo.se.lab1app.auth.domain.UserAccount;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
public class UserSyncToOneCService {

    private final boolean enabled;
    private final String syncUrl;
    private final String username;
    private final String password;
    private final HttpClient httpClient;

    public UserSyncToOneCService(
            @Value("${app.user-sync.enabled:true}") boolean enabled,
            @Value("${app.user-sync.url:http://93.100.213.224:1337/aviasales/hs/users/sync}") String syncUrl,
            @Value("${app.user-sync.username:api}") String username,
            @Value("${app.user-sync.password:123}") String password,
            @Value("${app.user-sync.connect-timeout-ms:5000}") int connectTimeoutMs,
            @Value("${app.user-sync.request-timeout-ms:10000}") int requestTimeoutMs
    ) {
        this.enabled = enabled;
        this.syncUrl = syncUrl;
        this.username = username;
        this.password = password;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(connectTimeoutMs))
                .build();
        this.requestTimeoutMs = requestTimeoutMs;
    }

    private final int requestTimeoutMs;

    public void syncUserBestEffort(UserAccount user) {
        if (!enabled || user == null) {
            return;
        }
        if (!StringUtils.hasText(syncUrl) || !StringUtils.hasText(username)) {
            log.warn("1C user sync is enabled but URL/username is not configured");
            return;
        }

        try {
            String payload = toPayload(user);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(syncUrl))
                    .timeout(Duration.ofMillis(requestTimeoutMs))
                    .header("Content-Type", "application/json; charset=utf-8")
                    .header("Authorization", basicAuthHeader())
                    .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn("Failed to sync user to 1C: status={} body={}", response.statusCode(), response.body());
            } else {
                log.info("User synced to 1C: username={}", user.getUsername());
            }
        } catch (Exception exception) {
            log.warn("Failed to sync user to 1C: username={}", user.getUsername(), exception);
        }
    }

    private String toPayload(UserAccount user) {
        String id = user.getId() == null ? "null" : String.valueOf(user.getId());
        String usernameValue = escapeJson(user.getUsername());
        String role = user.getRole() == null ? "" : user.getRole().name();
        return "{\"id\":" + id
                + ",\"username\":\"" + usernameValue + "\""
                + ",\"role\":\"" + escapeJson(role) + "\"}";
    }

    private String basicAuthHeader() {
        String credentials = username + ":" + (password == null ? "" : password);
        return "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
    }

    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }
}
