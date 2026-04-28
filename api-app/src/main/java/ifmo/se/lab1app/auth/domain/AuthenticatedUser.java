package ifmo.se.lab1app.auth.domain;

import ifmo.se.lab1app.shared.domain.UserRole;
import java.security.Principal;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Stream;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

public record AuthenticatedUser(
        String username,
        UserRole role,
        Set<String> privileges
) implements Principal {

    public AuthenticatedUser {
        privileges = Set.copyOf(privileges);
    }

    public static AuthenticatedUser fromRole(String username, UserRole role) {
        return new AuthenticatedUser(
                username,
                role,
                role.privileges().stream()
                        .map(Privilege::authority)
                        .collect(java.util.stream.Collectors.toUnmodifiableSet())
        );
    }

    public Collection<? extends GrantedAuthority> authorities() {
        return Stream.concat(Stream.of("ROLE_" + role.name()), privileges.stream())
                .distinct()
                .map(SimpleGrantedAuthority::new)
                .toList();
    }

    @Override
    public String getName() {
        return username;
    }
}
