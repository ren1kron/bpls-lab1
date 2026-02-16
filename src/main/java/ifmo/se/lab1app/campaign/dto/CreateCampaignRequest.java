package ifmo.se.lab1app.campaign.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CreateCampaignRequest(
    @NotBlank String name,
    @NotNull @DecimalMin("0.01") BigDecimal budgetAmount,
    @NotNull LocalDateTime startAt,
    @NotNull LocalDateTime endAt,
    Integer invoiceDueDays
) {

    @AssertTrue(message = "endAt must be after startAt")
    public boolean isValidRange() {
        return endAt == null || startAt == null || endAt.isAfter(startAt);
    }
}
