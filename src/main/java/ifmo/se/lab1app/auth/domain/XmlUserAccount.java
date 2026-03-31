package ifmo.se.lab1app.auth.domain;

import ifmo.se.lab1app.shared.domain.UserRole;

public record XmlUserAccount(
        String username,
        String password,
        UserRole role
) {}
