package ifmo.se.lab1app.campaign.dto;

import ifmo.se.lab1app.campaign.model.Campaign;
import ifmo.se.lab1app.campaign.model.CampaignStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

public record CampaignResponse(
    Long id,
    String name,
    String advertiserId,
    CampaignStatus status,
    BigDecimal budgetAmount,
    LocalDateTime startAt,
    LocalDateTime endAt,
    String configuration,
    String creatives,
    String validationComment,
    String moderationComment,
    Boolean budgetFormed,
    Integer invoiceDueDays,
    List<InvoiceResponse> invoices,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {

    public static CampaignResponse from(Campaign campaign) {
        List<InvoiceResponse> invoiceResponses = campaign.getInvoices().stream()
            .sorted(Comparator.comparingInt(invoice -> -invoice.getRevision()))
            .map(InvoiceResponse::from)
            .toList();

        return new CampaignResponse(
            campaign.getId(),
            campaign.getName(),
            campaign.getAdvertiserId(),
            campaign.getStatus(),
            campaign.getBudgetAmount(),
            campaign.getStartAt(),
            campaign.getEndAt(),
            campaign.getConfiguration(),
            campaign.getCreatives(),
            campaign.getValidationComment(),
            campaign.getModerationComment(),
            campaign.getBudgetFormed(),
            campaign.getInvoiceDueDays(),
            invoiceResponses,
            campaign.getCreatedAt(),
            campaign.getUpdatedAt()
        );
    }
}
