package ifmo.se.lab1app.client.api.dto;

import ifmo.se.lab1app.client.domain.enums.CampaignObjective;
import ifmo.se.lab1app.client.domain.enums.CampaignType;
import ifmo.se.lab1app.client.domain.enums.StartMode;
import ifmo.se.lab1app.shared.domain.Campaign;
import ifmo.se.lab1app.shared.domain.CampaignStatus;
import jakarta.persistence.Column;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record CampaignResponse(
    Long id,
    String name,
    CampaignObjective objective,
    CampaignType type,
    String url,
    StartMode startMode,
    CampaignStatus status,
    BigDecimal budgetAmount,
    LocalDateTime requestedStartAt,
    Integer durationDays,
    LocalDateTime actualStartAt,
    LocalDateTime actualEndAt,
    List<CreativeResponse> creatives,
    String moderationComment,
    String paymentConfirmationUrl,
    String paymentId,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {

    public static CampaignResponse from(Campaign campaign) {
        List<CreativeResponse> creativeResponses = campaign.getCreatives().stream()
            .map(CreativeResponse::from)
            .toList();

        return new CampaignResponse(
            campaign.getId(),
            campaign.getName(),
            campaign.getObjective(),
            campaign.getType(),
            campaign.getUrl(),
            campaign.getStartMode(),
            campaign.getStatus(),
            campaign.getBudgetAmount(),
            campaign.getRequestedStartAt(),
            campaign.getDurationDays(),
            campaign.getActualStartAt(),
            campaign.getActualEndAt(),
            creativeResponses,
            campaign.getModerationComment(),
            campaign.getPaymentConfirmationUrl(),
            campaign.getPaymentId(),
            campaign.getCreatedAt(),
            campaign.getUpdatedAt()
        );
    }
}
