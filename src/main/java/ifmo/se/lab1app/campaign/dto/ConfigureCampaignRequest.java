package ifmo.se.lab1app.campaign.dto;

import jakarta.validation.constraints.NotBlank;

public record ConfigureCampaignRequest(@NotBlank String configuration) {
}
