package ifmo.se.lab1app.campaign.dto;

import ifmo.se.lab1app.campaign.model.enums.CampaignObjective;
import ifmo.se.lab1app.campaign.model.enums.CampaignType;
import ifmo.se.lab1app.campaign.model.enums.StartMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record DraftCampaignRequest(
    @NotBlank String name,
    @NotNull CampaignObjective objective,
    @NotNull CampaignType campaignType,
    @NotNull StartMode startMode,
    @NotBlank String url,
    String notes
) {

//    @AssertTrue(message = "endAt must be after startAt")
//    public boolean isValidRange() {
//        return endAt == null || startAt == null || endAt.isAfter(startAt);
//    }
}
