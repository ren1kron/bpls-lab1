package ifmo.se.lab1app.billing.yookassa.application;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import ifmo.se.lab1app.exception.ExternalServiceException;
import ifmo.se.lab1app.shared.domain.Campaign;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
public class YooKassaPaymentClient {

    private static final String IDEMPOTENCE_KEY_HEADER = "Idempotence-Key";

    private final YooKassaProperties properties;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public YooKassaPaymentClient(YooKassaProperties properties) {
        this.properties = properties;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    public YooKassaPaymentResult createPayment(Campaign campaign) {
        validateConfiguration();

        YooKassaCreatePaymentResponse response;
        String idempotenceKey = UUID.randomUUID().toString();

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(trimTrailingSlash(properties.getApiUrl()) + "/payments"))
                    .timeout(Duration.ofSeconds(30))
                    .header("Authorization", basicAuthHeader())
                    .header(IDEMPOTENCE_KEY_HEADER, idempotenceKey)
                    .header("Content-Type", "application/json; charset=utf-8")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(
                            objectMapper.writeValueAsString(buildRequest(campaign)),
                            StandardCharsets.UTF_8
                    ))
                    .build();
            HttpResponse<String> httpResponse = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
            );
            if (httpResponse.statusCode() < 200 || httpResponse.statusCode() >= 300) {
                log.error("YooKassa returned {} for campaign {} body={}",
                        httpResponse.statusCode(), campaign.getId(), httpResponse.body());
                throw new ExternalServiceException("YooKassa не создала счет: " + httpResponse.statusCode());
            }
            response = objectMapper.readValue(httpResponse.body(), YooKassaCreatePaymentResponse.class);
        } catch (ExternalServiceException exception) {
            throw exception;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            log.error("YooKassa request failed for campaign {}", campaign.getId(), exception);
            throw new ExternalServiceException("Не удалось обратиться к YooKassa");
        } catch (Exception exception) {
            log.error("YooKassa request failed for campaign {}", campaign.getId(), exception);
            throw new ExternalServiceException("Не удалось обратиться к YooKassa");
        }

        if (response == null || !StringUtils.hasText(response.id())) {
            throw new ExternalServiceException("YooKassa вернула пустой ответ при создании счета");
        }
        if (response.confirmation() == null || !StringUtils.hasText(response.confirmation().confirmationUrl())) {
            throw new ExternalServiceException("YooKassa не вернула ссылку на оплату");
        }

        return new YooKassaPaymentResult(
            response.id(),
            response.status(),
            response.confirmation().confirmationUrl()
        );
    }

    private YooKassaCreatePaymentRequest buildRequest(Campaign campaign) {
        BigDecimal amount = campaign.getBudgetAmount();
        if (amount == null || amount.signum() <= 0) {
            throw new ExternalServiceException("Для кампании не задан корректный бюджет для выставления счета");
        }

        return new YooKassaCreatePaymentRequest(
            new Amount(amount.setScale(2, RoundingMode.HALF_UP).toPlainString(), "RUB"),
            true,
            new Confirmation("redirect", properties.getReturnUrl()),
            "Оплата рекламной кампании #" + campaign.getId() + " " + campaign.getName()
        );
    }

    private void validateConfiguration() {
        if (!properties.isEnabled()) {
            throw new ExternalServiceException("Интеграция с YooKassa отключена. Установите YOOKASSA_ENABLED=true");
        }
        if (!StringUtils.hasText(properties.getShopId()) || !StringUtils.hasText(properties.getSecretKey())) {
            throw new ExternalServiceException("Не заданы YOOKASSA_SHOP_ID или YOOKASSA_SECRET_KEY");
        }
        if (!StringUtils.hasText(properties.getReturnUrl())) {
            throw new ExternalServiceException("Не задан YOOKASSA_RETURN_URL");
        }
    }

    private String basicAuthHeader() {
        String credentials = properties.getShopId() + ":" + properties.getSecretKey();
        return "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
    }

    private String trimTrailingSlash(String value) {
        return value != null && value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private record YooKassaCreatePaymentRequest(
        Amount amount,
        boolean capture,
        Confirmation confirmation,
        String description
    ) {
    }

    private record Amount(
        String value,
        String currency
    ) {
    }

    private record Confirmation(
        String type,
        @JsonProperty("return_url") String returnUrl
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record YooKassaCreatePaymentResponse(
        String id,
        String status,
        ResponseConfirmation confirmation
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ResponseConfirmation(
        String type,
        @JsonProperty("confirmation_url") String confirmationUrl
    ) {
    }
}
