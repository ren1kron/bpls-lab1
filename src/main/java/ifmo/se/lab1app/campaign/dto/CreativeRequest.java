package ifmo.se.lab1app.campaign.dto;

import ifmo.se.lab1app.campaign.model.creative.CreativeType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreativeRequest(
    @NotBlank String name,
    @NotNull CreativeType type
) {
}
