package ifmo.se.lab1app.auth.jaas;

import ifmo.se.lab1app.auth.infra.UserAccountRepository;
import java.util.Map;
import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DatabaseUserJaasConfiguration extends Configuration {

    public static final String LOGIN_CONTEXT_NAME = "lab1-app";
    static final String USER_ACCOUNT_REPOSITORY_OPTION = "userAccountRepository";
    static final String PASSWORD_ENCODER_OPTION = "passwordEncoder";

    private final AppConfigurationEntry[] entries;

    public DatabaseUserJaasConfiguration(
            UserAccountRepository userAccountRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.entries = new AppConfigurationEntry[] {
                new AppConfigurationEntry(
                        DatabaseUserLoginModule.class.getName(),
                        AppConfigurationEntry.LoginModuleControlFlag.REQUIRED,
                        Map.of(
                                USER_ACCOUNT_REPOSITORY_OPTION, userAccountRepository,
                                PASSWORD_ENCODER_OPTION, passwordEncoder
                        )
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
