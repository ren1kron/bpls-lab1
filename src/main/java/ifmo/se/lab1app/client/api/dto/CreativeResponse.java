package ifmo.se.lab1app.client.api.dto;

import ifmo.se.lab1app.client.domain.creative.Creative;
import ifmo.se.lab1app.client.domain.creative.CreativeType;

public record CreativeResponse(
    Long id,
    String name,
    CreativeType type
) {

    public static CreativeResponse from(Creative creative) {
        return new CreativeResponse(
            creative.getId(),
            creative.getName(),
            creative.getType()
        );
    }
}
