package ifmo.se.lab1app.moderator.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ModerationDecisionRequest(@NotNull Boolean approved, @Size(max = 4000) String comment) {
}
