package ifmo.se.lab1app.client.api.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ReconfigureCampaignRequest(
        @Positive @Digits(integer = 13, fraction = 2) BigDecimal budgetAmount,
        @Future LocalDateTime requestedStartAt,
        @Positive Integer durationDays
) {}
