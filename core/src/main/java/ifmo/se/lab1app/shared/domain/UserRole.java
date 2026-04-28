package ifmo.se.lab1app.shared.domain;

import ifmo.se.lab1app.auth.domain.Privilege;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public enum UserRole {
    CLIENT(Set.of(
            Privilege.CAMPAIGN_VIEW,
            Privilege.CAMPAIGN_CREATE,
            Privilege.CAMPAIGN_EDIT,
            Privilege.CAMPAIGN_CONFIGURE,
            Privilege.CREATIVE_MANAGE,
            Privilege.CAMPAIGN_SUBMIT,
            Privilege.CAMPAIGN_DELETE,
            Privilege.CAMPAIGN_FREEZE,
            Privilege.CAMPAIGN_PROCEED
    )),
    COMPANY_MODERATOR(Set.of(
            Privilege.CAMPAIGN_VIEW,
            Privilege.CAMPAIGN_MODERATE,
            Privilege.SCHEDULER_RUN
    ));

    private final Set<Privilege> privileges;

    UserRole(Set<Privilege> privileges) {
        this.privileges = Set.copyOf(privileges);
    }

    public Set<Privilege> privileges() {
        return privileges;
    }

    public Set<String> grantedAuthorities() {
        LinkedHashSet<String> authorities = new LinkedHashSet<>();
        authorities.add("ROLE_" + name());
        privileges.stream()
                .map(Privilege::authority)
                .forEach(authorities::add);
        return Collections.unmodifiableSet(authorities);
    }
}
