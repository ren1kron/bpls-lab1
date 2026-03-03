package ifmo.se.lab1app.client.api.dto;

import jakarta.validation.constraints.NotNull;

public record ResumeDecisionRequest(
    @NotNull Boolean resume,
    @NotNull Boolean budgetFormed
) {
}
