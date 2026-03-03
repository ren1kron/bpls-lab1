package ifmo.se.lab1app.client.api.dto;

import ifmo.se.lab1app.shared.domain.Campaign;
import ifmo.se.lab1app.shared.domain.CampaignStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

public record CampaignResponse(
    Long id,
    String name,
    CampaignStatus status,
    BigDecimal budgetAmount,
    LocalDateTime startAt,
    LocalDateTime endAt,
    String configuration,
    List<CreativeResponse> creatives,
    String validationComment,
    String moderationComment,
    Integer invoiceDueDays,
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
            campaign.getStatus(),
            campaign.getBudgetAmount(),
            campaign.getStartAt(),
            campaign.getEndAt(),
            campaign.getConfiguration(),
            creativeResponses,
            campaign.getValidationComment(),
            campaign.getModerationComment(),
            campaign.getInvoiceDueDays(),
            campaign.getCreatedAt(),
            campaign.getUpdatedAt()
        );
    }
}
