package ifmo.se.lab1app.auth.api.dto;

import ifmo.se.lab1app.shared.domain.UserRole;
import java.util.Set;

public record UserResponse(
        String username,
        UserRole role,
        Set<String> privileges
) {}
