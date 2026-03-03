package ifmo.se.lab1app.moderator.api.dto;

import jakarta.validation.constraints.NotNull;

public record ModerationDecisionRequest(@NotNull Boolean approved, String comment) {
}
