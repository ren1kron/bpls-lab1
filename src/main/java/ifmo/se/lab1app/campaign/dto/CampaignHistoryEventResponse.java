package ifmo.se.lab1app.campaign.dto;

import ifmo.se.lab1app.campaign.model.CampaignEventType;
import ifmo.se.lab1app.campaign.model.CampaignHistoryEvent;
import java.time.LocalDateTime;

public record CampaignHistoryEventResponse(
    Long id,
    CampaignEventType type,
    String details,
    LocalDateTime createdAt
) {

    public static CampaignHistoryEventResponse from(CampaignHistoryEvent event) {
        return new CampaignHistoryEventResponse(
            event.getId(),
            event.getType(),
            event.getDetails(),
            event.getCreatedAt()
        );
    }
}
