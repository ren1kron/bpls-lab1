package ifmo.se.lab1app.moderator.api;

import ifmo.se.lab1app.client.api.dto.CampaignResponse;
import ifmo.se.lab1app.moderator.api.dto.ModerationDecisionRequest;
import ifmo.se.lab1app.moderator.application.ModeratorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/moderator")
@PreAuthorize("hasRole('COMPANY_MODERATOR')")
@RequiredArgsConstructor
@Tag(
        name = "Moderator workflow",
        description = "Интерфейс модерации рекламных компаний"
)
public class ModeratorController {

    private final ModeratorService moderatorService;


    @PostMapping("/{campaignId}")
    @Operation(summary = "Ручная модерация")
    public CampaignResponse processModerationDecision(
            @Parameter(description = "Идентификатор экземпляра процесса (campaignId)")
            @PathVariable Long campaignId,
            @Valid @RequestBody ModerationDecisionRequest request) {
        return moderatorService.processModerationDecision(campaignId, request);
    }
}
