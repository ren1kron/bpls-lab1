package ifmo.se.lab1app.campaign.dto;

import ifmo.se.lab1app.campaign.model.UserRole;

public record UserResponse(
        Long id,
        String username,
        UserRole role
) {}
