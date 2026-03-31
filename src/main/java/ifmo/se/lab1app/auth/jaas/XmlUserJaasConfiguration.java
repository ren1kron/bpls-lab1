package ifmo.se.lab1app.auth.jaas;

import java.util.Map;
import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class XmlUserJaasConfiguration extends Configuration {

    public static final String LOGIN_CONTEXT_NAME = "lab1-app";
    public static final String USERS_XML_LOCATION_OPTION = "usersXmlLocation";

    private final AppConfigurationEntry[] entries;

    public XmlUserJaasConfiguration(
            @Value("${app.security.jaas.users-xml:classpath:security/users.xml}") String usersXmlLocation
    ) {
        this.entries = new AppConfigurationEntry[] {
                new AppConfigurationEntry(
                        XmlUserLoginModule.class.getName(),
                        AppConfigurationEntry.LoginModuleControlFlag.REQUIRED,
                        Map.of(USERS_XML_LOCATION_OPTION, usersXmlLocation)
                )
        };
    }

    @Override
    public AppConfigurationEntry[] getAppConfigurationEntry(String name) {
        if (!LOGIN_CONTEXT_NAME.equals(name)) {
            return null;
        }
        return entries.clone();
    }
}
