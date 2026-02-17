package ifmo.se.lab1app.campaign.dto;

import ifmo.se.lab1app.campaign.model.creative.Creative;
import ifmo.se.lab1app.campaign.model.creative.CreativeType;

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
