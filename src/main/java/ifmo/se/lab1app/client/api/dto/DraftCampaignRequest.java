package ifmo.se.lab1app.client.api.dto;

import ifmo.se.lab1app.client.domain.enums.CampaignObjective;
import ifmo.se.lab1app.client.domain.enums.CampaignType;
import ifmo.se.lab1app.client.domain.enums.StartMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record DraftCampaignRequest(
    @NotBlank String name,
    @NotNull CampaignObjective objective,
    @NotNull CampaignType campaignType,
    @NotNull StartMode startMode,
    @NotBlank String url,
    String notes
) {}
