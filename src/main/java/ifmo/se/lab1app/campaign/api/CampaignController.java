package ifmo.se.lab1app.campaign.api;

import ifmo.se.lab1app.campaign.dto.*;
import ifmo.se.lab1app.campaign.dto.DraftCampaignRequest;
import ifmo.se.lab1app.campaign.service.CampaignWorkflowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/campaigns")
@RequiredArgsConstructor
@Tag(
    name = "Campaign Workflow",
    description = "Операции управления жизненным циклом рекламной кампании по BPMN-процессу Process_CampaignLaunch"
)
public class CampaignController {

    private final CampaignWorkflowService campaignWorkflowService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Создать черновик кампании")
    public CampaignResponse createCampaign(@Valid @RequestBody DraftCampaignRequest request) {
        return campaignWorkflowService.createCampaign(request);
    }

    @PutMapping("/{campaignId}")
    @Operation(summary = "Обновить черновик кампании")
    public CampaignResponse updateCampaignDraft(
        @Parameter(description = "Идентификатор экземпляра процесса (campaignId)")
        @PathVariable Long campaignId,
        @Valid @RequestBody DraftCampaignRequest request
    ) {
        return campaignWorkflowService.updateCampaignDraft(campaignId, request);
    }

    @GetMapping
    @Operation(summary = "Получить список кампаний")
    public List<CampaignResponse> getCampaigns() {
        return campaignWorkflowService.getCampaigns();
    }

    @GetMapping("/{campaignId}")
    @Operation(summary = "Получить кампанию по ID")
    public CampaignResponse getCampaign(
        @Parameter(description = "Идентификатор экземпляра процесса (campaignId)")
        @PathVariable Long campaignId
    ) {
        return campaignWorkflowService.getCampaign(campaignId);
    }

    @GetMapping("/{campaignId}/history")
    @Operation(summary = "Получить историю переходов кампании")
    public List<CampaignHistoryEventResponse> getCampaignHistory(
        @Parameter(description = "Идентификатор экземпляра процесса (campaignId)")
        @PathVariable Long campaignId
    ) {
        return campaignWorkflowService.getCampaignHistory(campaignId);
    }

    @PostMapping("/{campaignId}/configure")
    @Operation(summary = "Настроить кампанию")
    public CampaignResponse configureCampaign(
        @Parameter(description = "Идентификатор экземпляра процесса (campaignId)")
        @PathVariable Long campaignId,
        @Valid @RequestBody ConfigureCampaignRequest request
    ) {
        return campaignWorkflowService.configureCampaign(campaignId, request);
    }

    @PostMapping("/{campaignId}/creatives")
    @Operation(summary = "Загрузить креативы")
    public CampaignResponse uploadCreatives(
        @Parameter(description = "Идентификатор экземпляра процесса (campaignId)")
        @PathVariable Long campaignId,
        @Valid @RequestBody UploadCreativesRequest request
    ) {
        return campaignWorkflowService.uploadCreatives(campaignId, request);
    }

    @PostMapping("/{campaignId}/submit")
    @Operation(summary = "Отправить кампанию на проверку")
    public CampaignResponse submitForCheck(
        @Parameter(description = "Идентификатор экземпляра процесса (campaignId)")
        @PathVariable Long campaignId
    ) {
        return campaignWorkflowService.submitForCheck(campaignId);
    }

    @PostMapping("/{campaignId}/validation")
    @Operation(summary = "Зафиксировать результат автоматической валидации")
    public CampaignResponse processValidation(
        @Parameter(description = "Идентификатор экземпляра процесса (campaignId)")
        @PathVariable Long campaignId,
        @Valid @RequestBody ValidationResultRequest request
    ) {
        return campaignWorkflowService.processValidationResult(campaignId, request);
    }

    @PostMapping("/{campaignId}/validation/fix")
    @Operation(summary = "Исправить ошибки валидации")
    public CampaignResponse fixValidationIssues(
        @Parameter(description = "Идентификатор экземпляра процесса (campaignId)")
        @PathVariable Long campaignId
    ) {
        return campaignWorkflowService.fixValidationIssues(campaignId);
    }

    @PostMapping("/{campaignId}/moderation")
    @Operation(summary = "Зафиксировать решение модерации")
    public CampaignResponse processModeration(
        @Parameter(description = "Идентификатор экземпляра процесса (campaignId)")
        @PathVariable Long campaignId,
        @Valid @RequestBody ModerationDecisionRequest request
    ) {
        return campaignWorkflowService.processModerationDecision(campaignId, request);
    }

    @PostMapping("/{campaignId}/moderation/fix")
    @Operation(summary = "Исправить замечания модерации")
    public CampaignResponse fixModerationIssues(
        @Parameter(description = "Идентификатор экземпляра процесса (campaignId)")
        @PathVariable Long campaignId
    ) {
        return campaignWorkflowService.fixModerationIssues(campaignId);
    }

    @PostMapping("/{campaignId}/billing/payment-received")
    @Operation(summary = "Зафиксировать оплату счёта")
    public CampaignResponse markPaymentReceived(
        @Parameter(description = "Идентификатор экземпляра процесса (campaignId)")
        @PathVariable Long campaignId,
        @RequestBody(required = false) PaymentReceivedRequest request
    ) {
        PaymentReceivedRequest payload = request == null ? new PaymentReceivedRequest(null) : request;
        return campaignWorkflowService.markPaymentReceived(campaignId, payload);
    }

    @PostMapping("/{campaignId}/billing/due-date-reached")
    @Operation(summary = "Зафиксировать истечение срока оплаты")
    public CampaignResponse markDueDateReached(
        @Parameter(description = "Идентификатор экземпляра процесса (campaignId)")
        @PathVariable Long campaignId
    ) {
        return campaignWorkflowService.markInvoiceDueDateReached(campaignId);
    }

    @PostMapping("/{campaignId}/pause")
    @Operation(summary = "Поставить активную кампанию на паузу")
    public CampaignResponse requestPause(
        @Parameter(description = "Идентификатор экземпляра процесса (campaignId)")
        @PathVariable Long campaignId
    ) {
        return campaignWorkflowService.requestPause(campaignId);
    }

    @PostMapping("/{campaignId}/resume")
    @Operation(summary = "Решить: возобновить кампанию или остановить")
    public CampaignResponse decideResumeOrStop(
        @Parameter(description = "Идентификатор экземпляра процесса (campaignId)")
        @PathVariable Long campaignId,
        @Valid @RequestBody ResumeDecisionRequest request
    ) {
        return campaignWorkflowService.decideResumeOrStop(campaignId, request);
    }

    @PostMapping("/{campaignId}/events/budget-exhausted")
    @Operation(summary = "Системное событие: бюджет исчерпан")
    public CampaignResponse markBudgetExhausted(
        @Parameter(description = "Идентификатор экземпляра процесса (campaignId)")
        @PathVariable Long campaignId
    ) {
        return campaignWorkflowService.markBudgetExhausted(campaignId);
    }

    @PostMapping("/{campaignId}/events/end-at-reached")
    @Operation(summary = "Системное событие: достигнут endAt")
    public CampaignResponse markEndAtReached(
        @Parameter(description = "Идентификатор экземпляра процесса (campaignId)")
        @PathVariable Long campaignId
    ) {
        return campaignWorkflowService.markEndAtReached(campaignId);
    }

    @PostMapping("/{campaignId}/timers/tick")
    @Operation(summary = "Запустить обработку таймеров для кампании")
    public CampaignResponse processCampaignTimers(
        @Parameter(description = "Идентификатор экземпляра процесса (campaignId)")
        @PathVariable Long campaignId
    ) {
        return campaignWorkflowService.processTimersForCampaign(campaignId);
    }

    @PostMapping("/timers/tick")
    @Operation(summary = "Запустить обработку таймеров для всех кампаний")
    public void processAllTimers() {
        campaignWorkflowService.processTimersForAllCampaigns();
    }
}
