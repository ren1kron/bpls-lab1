package ifmo.se.lab1app.client.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record UploadCreativesRequest(
    @NotEmpty
    @Size(max = 10, message = "creatives must contain at most 10 items")
    List<@Valid CreativeRequest> creatives
) {
}
