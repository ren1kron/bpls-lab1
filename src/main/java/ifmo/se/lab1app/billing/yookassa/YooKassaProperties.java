package ifmo.se.lab1app.billing.yookassa;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.yookassa")
public class YooKassaProperties {

    private boolean enabled = false;
    private String apiUrl = "https://api.yookassa.ru/v3";
    private String shopId;
    private String secretKey;
    private String returnUrl;
}
