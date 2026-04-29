package ifmo.se.lab1app.eis;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.eis")
public record EisIntegrationProperties(
        boolean enabled,
        String connectionFactoryJndiName,
        boolean failOnError
) {

    public EisIntegrationProperties {
        if (connectionFactoryJndiName == null || connectionFactoryJndiName.isBlank()) {
            connectionFactoryJndiName = "java:/eis/OneCConnectionFactory";
        }
    }
}
