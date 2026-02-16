package ifmo.se.lab1app.campaign.dto;

import jakarta.validation.constraints.NotBlank;

public record UploadCreativesRequest(@NotBlank String creatives) {
}
