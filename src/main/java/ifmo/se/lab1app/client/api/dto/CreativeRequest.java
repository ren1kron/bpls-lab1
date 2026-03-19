package ifmo.se.lab1app.client.api.dto;

import ifmo.se.lab1app.client.domain.creative.CreativeType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.URL;

public record CreativeRequest(
    @NotBlank @URL String name,
    @NotNull CreativeType type
) {
}
