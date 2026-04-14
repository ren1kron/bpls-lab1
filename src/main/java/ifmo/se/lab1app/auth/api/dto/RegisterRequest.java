package ifmo.se.lab1app.auth.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Size(max = 255) String username,
        @NotBlank @Size(min = 6, max = 255) String password
) {}
