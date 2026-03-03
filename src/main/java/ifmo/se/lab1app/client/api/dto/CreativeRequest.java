package ifmo.se.lab1app.client.api.dto;

import ifmo.se.lab1app.client.domain.creative.CreativeType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreativeRequest(
    @NotBlank String name,
    @NotNull CreativeType type
) {
}
