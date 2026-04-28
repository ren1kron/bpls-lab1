package ifmo.se.lab1app.client.api.dto;

import ifmo.se.lab1app.client.domain.enums.CampaignObjective;
import ifmo.se.lab1app.client.domain.enums.CampaignType;
import ifmo.se.lab1app.client.domain.enums.StartMode;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

public record UpdateDraftCampaignRequest(
        @Size(max = 255) String name,
        CampaignObjective objective,
        CampaignType campaignType,
        StartMode startMode,
        @URL(protocol = "https", regexp = "^(https://).+\\..+") String url
) {}
