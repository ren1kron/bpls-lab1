package ifmo.se.lab1app.client.api.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ConfigureCampaignRequest(
        @NotNull BigDecimal budgetAmount,
        @NotNull LocalDateTime startAt,
        @NotNull LocalDateTime endAt,
        String notes
) {
    @AssertTrue(message = "endAt must be after startAt")
    public boolean isValidRange() {
        return endAt == null || startAt == null || endAt.isAfter(startAt);
    }
}
