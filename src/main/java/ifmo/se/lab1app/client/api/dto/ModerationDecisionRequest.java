package ifmo.se.lab1app.client.api.dto;

import jakarta.validation.constraints.NotNull;

public record ModerationDecisionRequest(@NotNull Boolean approved, String comment) {
}
