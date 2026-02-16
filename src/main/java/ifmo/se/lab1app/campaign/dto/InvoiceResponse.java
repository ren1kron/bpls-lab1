package ifmo.se.lab1app.campaign.dto;

import ifmo.se.lab1app.campaign.model.Invoice;
import ifmo.se.lab1app.campaign.model.InvoiceStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record InvoiceResponse(
    Long id,
    Integer revision,
    InvoiceStatus status,
    BigDecimal amount,
    LocalDateTime issuedAt,
    LocalDateTime dueAt,
    LocalDateTime paidAt
) {

    public static InvoiceResponse from(Invoice invoice) {
        return new InvoiceResponse(
            invoice.getId(),
            invoice.getRevision(),
            invoice.getStatus(),
            invoice.getAmount(),
            invoice.getIssuedAt(),
            invoice.getDueAt(),
            invoice.getPaidAt()
        );
    }
}
