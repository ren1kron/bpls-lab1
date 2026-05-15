package ifmo.se.lab1app.camunda;

import ifmo.se.lab1app.auth.domain.UserAccount;
import ifmo.se.lab1app.shared.domain.UserRole;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class CamundaIdentitySyncService {

    private final CamundaProperties properties;
    private final CamundaRestClient camunda;

    public CamundaIdentitySyncService(CamundaProperties properties, CamundaRestClient camunda) {
        this.properties = properties;
        this.camunda = camunda;
    }

    public void syncUserBestEffort(UserAccount user, String rawPassword) {
        if (!properties.isEnabled() || !properties.isIdentitySyncEnabled() || user == null) {
            return;
        }
        try {
            ensureRoleGroups();
            camunda.ensureUser(user.getUsername(), rawPassword);
            camunda.addUserToGroup(user.getUsername(), camundaGroupId(user.getRole()));
        } catch (Exception exception) {
            log.warn("Failed to sync user {} to Camunda identity service", user.getUsername(), exception);
        }
    }

    private void ensureRoleGroups() {
        for (UserRole role : UserRole.values()) {
            camunda.ensureGroup(camundaGroupId(role), role.name());
        }
    }

    private String camundaGroupId(UserRole role) {
        return role.name().replaceAll("[^A-Za-z0-9]", "");
    }
}
