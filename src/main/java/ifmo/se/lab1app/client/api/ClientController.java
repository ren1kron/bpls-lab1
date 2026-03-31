package ifmo.se.lab1app.client.api;

import ifmo.se.lab1app.client.api.dto.*;
import ifmo.se.lab1app.client.application.ClientWorkflowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/advertisement/client")
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
        return clientService.createDraft(request);
    }

    @PatchMapping("/{campaignId}")
    @Operation(summary = "Обновить черновик кампании")
    public CampaignResponse patchCampaignDraft(
            @Parameter(description = "Идентификатор экземпляра процесса (campaignId)")
            @PathVariable Long campaignId,
            @Valid @RequestBody UpdateDraftCampaignRequest request
    ) {
        return clientService.patchCampaignBasics(campaignId, request);
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

    @PatchMapping("/{campaignId}/configure")
    @Operation(summary = "Перенастроить кампанию (плейсменты/таргетинг/расписание/бюджет)")
    public CampaignResponse reconfigureCampaign(
            @Parameter(description = "Идентификатор экземпляра процесса (campaignId)")
            @PathVariable Long campaignId,
            @Valid @RequestBody ReconfigureCampaignRequest request
    ) {
        return clientService.reconfigureCampaign(campaignId, request);
    }

    @PostMapping("/{campaignId}/creatives")
    @Operation(summary = "Добавить кампании один креатив")
    public CampaignResponse uploadCreative(
            @Parameter(description = "Идентификатор экземпляра процесса (campaignId)")
            @PathVariable Long campaignId,
            @Valid @RequestBody CreativeRequest request
    ) {
        return clientService.addCreative(campaignId, request);
    }

    @DeleteMapping("/{campaignId}/creatives/{creativeId}")
    @Operation(summary = "Удалить один креатив у кампании")
    public CampaignResponse deleteCreative(
            @PathVariable Long campaignId,
            @PathVariable Long creativeId
    ) {
        return clientService.deleteCreative(campaignId, creativeId);
    }

    @PostMapping("/{campaignId}/submit")
    @Operation(summary = "Отправить на модерацию")
    public CampaignResponse submitForCheck(
            @Parameter(description = "Идентификатор экземпляра процесса (campaignId)")
            @PathVariable Long campaignId
    ) {
        return clientService.submitForCheck(campaignId);
    }

    @DeleteMapping("/{campaignId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Удалить кампанию (можно удалить только не запущенную кампанию)")
    public void deleteCampaign(
            @Parameter(description = "Идентификатор экземпляра процесса (campaignId)")
            @PathVariable Long campaignId
    ) {
        clientService.deleteCampaign(campaignId);
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
