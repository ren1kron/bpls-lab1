package ifmo.se.lab1app.client.api.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ConfigureCampaignRequest(
        @NotNull @Positive @Digits(integer = 13, fraction = 2) BigDecimal budgetAmount,
        @NotNull @Future LocalDateTime requestedStartAt,
        @NotNull @Positive @Max(365) Integer durationDays
) {}
