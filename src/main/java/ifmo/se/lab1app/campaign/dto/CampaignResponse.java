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
    CampaignStatus status,
    BigDecimal budgetAmount,
    LocalDateTime startAt,
    LocalDateTime endAt,
    String configuration,
    List<CreativeResponse> creatives,
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
            campaign.getBudgetFormed(),
            campaign.getInvoiceDueDays(),
            invoiceResponses,
            campaign.getCreatedAt(),
            campaign.getUpdatedAt()
        );
    }
}
