package ifmo.se.lab1app.eis;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.eis")
public record EisIntegrationProperties(
        boolean enabled,
        String connectionFactoryJndiName,
        boolean failOnError,
        int outboxBatchSize,
        long outboxPollIntervalMs
) {

    public EisIntegrationProperties {
        if (connectionFactoryJndiName == null || connectionFactoryJndiName.isBlank()) {
            connectionFactoryJndiName = "java:/eis/OneCConnectionFactory";
        }
        if (outboxBatchSize <= 0) {
            outboxBatchSize = 25;
        }
        if (outboxPollIntervalMs <= 0) {
            outboxPollIntervalMs = 1000;
        }
    }
}
