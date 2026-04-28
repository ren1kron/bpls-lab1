package ifmo.se.lab1app.billing.yookassa.application;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import ifmo.se.lab1app.exception.ExternalServiceException;
import ifmo.se.lab1app.shared.domain.Campaign;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Slf4j
@Component
public class YooKassaPaymentClient {

    private static final String IDEMPOTENCE_KEY_HEADER = "Idempotence-Key";

    private final YooKassaProperties properties;

    public YooKassaPaymentClient(YooKassaProperties properties) {
        this.properties = properties;
    }

    public YooKassaPaymentResult createPayment(Campaign campaign) {
        validateConfiguration();

        YooKassaCreatePaymentResponse response;
        String idempotenceKey = UUID.randomUUID().toString();

        try {
            response = RestClient.builder()
                .baseUrl(properties.getApiUrl())
                .defaultHeaders(headers -> headers.setBasicAuth(properties.getShopId(), properties.getSecretKey()))
                .build()
                .post()
                .uri("/payments")
                .header(IDEMPOTENCE_KEY_HEADER, idempotenceKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(buildRequest(campaign))
                .retrieve()
                .body(YooKassaCreatePaymentResponse.class);
        } catch (RestClientResponseException exception) {
            log.error("YooKassa returned {} for campaign {}", exception.getStatusCode(), campaign.getId(), exception);
            throw new ExternalServiceException("YooKassa не создала счет: " + exception.getStatusCode().value());
        } catch (RestClientException exception) {
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
