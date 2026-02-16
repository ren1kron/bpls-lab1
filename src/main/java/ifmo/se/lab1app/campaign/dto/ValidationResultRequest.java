package ifmo.se.lab1app.campaign.dto;

import jakarta.validation.constraints.NotNull;

public record ValidationResultRequest(@NotNull Boolean validationOk, String comment) {
}
