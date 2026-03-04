package ifmo.se.lab1app.client.api;

import ifmo.se.lab1app.client.api.dto.*;
import ifmo.se.lab1app.client.application.ClientWorkflowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@PreAuthorize("hasRole('CLIENT')")
@RestController
@RequestMapping("/api/client")
@RequiredArgsConstructor
@Tag(
        name = "Client workflow",
        description = "Интерфейс настройки рекламной компании пользователем"
)
public class ClientController {

    private final ClientWorkflowService clientService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Создать черновик кампании")
    public CampaignResponse createCampaignDraft(@Valid @RequestBody DraftCampaignRequest request) {
        return clientService.createCampaignDraft(request);
    }

    @PutMapping("/{campaignId}")
    @Operation(summary = "Обновить черновик кампании")
    public CampaignResponse updateCampaignDraft(
            @Parameter(description = "Идентификатор экземпляра процесса (campaignId)")
            @PathVariable Long campaignId,
            @Valid @RequestBody DraftCampaignRequest request
    ) {
        return clientService.updateCampaignDraft(campaignId, request);
    }

    @PostMapping("/{campaignId}/configure")
    @Operation(summary = "Настроить кампанию (плейсменты/таргетинг/расписание/бюджет)")
    public CampaignResponse configureCampaign(
            @Parameter(description = "Идентификатор экземпляра процесса (campaignId)")
            @PathVariable Long campaignId,
            @Valid @RequestBody ConfigureCampaignRequest request
    ) {
        return clientService.configureCampaign(campaignId, request);
    }

    @PostMapping("/{campaignId}/creatives")
    @Operation(summary = "Загрузить креативы")
    public CampaignResponse uploadCreatives(
            @Parameter(description = "Идентификатор экземпляра процесса (campaignId)")
            @PathVariable Long campaignId,
            @Valid @RequestBody UploadCreativesRequest request
    ) {
        return clientService.uploadCreatives(campaignId, request);
    }

    @PostMapping("/{campaignId}/submit")
    @Operation(summary = "Отправить на модерацию")
    public CampaignResponse submitForCheck(
            @Parameter(description = "Идентификатор экземпляра процесса (campaignId)")
            @PathVariable Long campaignId
    ) {
        return clientService.submitForCheck(campaignId);
    }

    @PostMapping("/{campaignId}/fix")
    @Operation(summary = "Поправить по замечаниям модерации")
    public CampaignResponse fixModerationIssues(
            @Parameter(description = "Идентификатор экземпляра процесса (campaignId)")
            @PathVariable Long campaignId,
            @Valid @RequestBody UploadCreativesRequest request
    ) {
        return clientService.fixModerationIssues(campaignId, request);
    }


    @PostMapping("/{campaignId}/stop")
    @Operation(summary = "Запросить паузу")
    public CampaignResponse freezeCampaign(
            @Parameter(description = "Идентификатор экземпляра процесса (campaignId)")
            @PathVariable Long campaignId
    ) {
        return clientService.freezeCampaign(campaignId);
    }

    @PostMapping("/{campaignId}/proceed")
    @Operation(summary = "Решить: запустить или завершить")
    public CampaignResponse proceedCampaign(
            @Parameter(description = "Идентификатор экземпляра процесса (campaignId)")
            @PathVariable Long campaignId,
            @Valid @RequestBody ProceedCampaignRequest request
            ) {
        return clientService.restartCampaign(campaignId, request);
    }
}
