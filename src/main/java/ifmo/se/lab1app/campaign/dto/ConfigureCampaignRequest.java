package ifmo.se.lab1app.campaign.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ConfigureCampaignRequest(
        @NotNull BigDecimal budgetAmount,
        @NotNull LocalDateTime startAt,
        @NotNull LocalDateTime endAt,
        String notes
) {
}
