package ifmo.se.lab1app.auth.api.dto;

public record LoginResponse(
        String accessToken,
        String tokenType,
        UserResponse user
) {}
