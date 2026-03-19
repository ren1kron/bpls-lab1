package ifmo.se.lab1app.client.api.dto;

import ifmo.se.lab1app.client.domain.enums.CampaignObjective;
import ifmo.se.lab1app.client.domain.enums.CampaignType;
import ifmo.se.lab1app.client.domain.enums.StartMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

public record DraftCampaignRequest(
    @NotBlank @Size(max = 255) String name,
    @NotNull CampaignObjective objective,
    @NotNull CampaignType campaignType,
    @NotNull StartMode startMode,
    @NotBlank @URL(protocol = "https") String url
) {}
