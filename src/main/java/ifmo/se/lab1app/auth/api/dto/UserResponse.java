package ifmo.se.lab1app.auth.api.dto;

import ifmo.se.lab1app.shared.domain.UserRole;

public record UserResponse(
        Long id,
        String username,
        UserRole role
) {}
