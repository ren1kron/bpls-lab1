package ifmo.se.lab1app.client.api.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ReconfigureCampaignRequest(
        @Positive @Digits(integer = 13, fraction = 2) BigDecimal budgetAmount,
        @Future LocalDateTime requestedStartAt,
        @Positive @Max(365) Integer durationDays
) {}
