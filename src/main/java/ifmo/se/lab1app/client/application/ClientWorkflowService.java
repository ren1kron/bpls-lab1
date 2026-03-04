package ifmo.se.lab1app.client.application;

import ifmo.se.lab1app.client.api.dto.*;
import ifmo.se.lab1app.client.domain.creative.Creative;
import ifmo.se.lab1app.shared.infra.CampaignRepository;
import ifmo.se.lab1app.exception.InvalidStateException;
import ifmo.se.lab1app.exception.NotFoundException;
import ifmo.se.lab1app.shared.domain.Campaign;
import ifmo.se.lab1app.shared.domain.CampaignStatus;
import jakarta.validation.Valid;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@Transactional
public class ClientWorkflowService {

    private final CampaignRepository campaignRepository;

    public ClientWorkflowService(CampaignRepository campaignRepository, CampaignRepository campaignRepo) {
        this.campaignRepository = campaignRepository;
    }

    // 1. создать черновик кампании
    public CampaignResponse createCampaignDraft(DraftCampaignRequest request) {
        Campaign campaign = new Campaign();
        campaign.setName(request.name());
        campaign.setObjective(request.objective());
        campaign.setType(request.campaignType());
        campaign.setStartMode(request.startMode());
        campaign.setUrl(request.url());
        campaign.setNotes(request.notes());
        campaign.setStatus(CampaignStatus.DRAFT);

        return CampaignResponse.from(campaignRepository.save(campaign));
    }

    // 2. обновить черновик кампании
    public CampaignResponse updateCampaignDraft(Long campaignId, DraftCampaignRequest request) {
        Campaign campaign = findCampaign(campaignId);
        requireStatus(campaign, CampaignStatus.DRAFT);

        campaign.setName(request.name());
        campaign.setObjective(request.objective());
        campaign.setType(request.campaignType());
        campaign.setStartMode(request.startMode());
        campaign.setUrl(request.url());
        campaign.setNotes(request.notes());

        return CampaignResponse.from(campaignRepository.save(campaign));
    }

    // 3. настроить кампанию (плейсменты/таргетинг/расписание/бюджет)
    public CampaignResponse configureCampaign(Long campaignId, ConfigureCampaignRequest request) {
        Campaign campaign = findCampaign(campaignId);
        requireStatus(campaign, CampaignStatus.DRAFT);

        campaign.setBudgetAmount(request.budgetAmount());
        campaign.setStartAt(request.startAt());
        campaign.setEndAt(request.endAt());

        campaign.setStatus(CampaignStatus.CONFIGURED);

        return CampaignResponse.from(campaignRepository.save(campaign));
    }

    // 4. Загрузить креативы
    public CampaignResponse uploadCreatives(Long campaignId, UploadCreativesRequest request) {
        Campaign campaign = findCampaign(campaignId);
        requireStatus(campaign, CampaignStatus.CONFIGURED, CampaignStatus.CREATIVES_UPLOADED);

        campaign.getCreatives().clear();
        List<Creative> creatives = request.creatives().stream()
                .map(creativeRequest -> {
                    Creative creative = new Creative();
                    creative.setName(creativeRequest.name());
                    creative.setType(creativeRequest.type());
                    creative.setCampaign(campaign);
                    return creative;
                })
                .toList();
        campaign.getCreatives().addAll(creatives);

        campaign.setStatus(CampaignStatus.CREATIVES_UPLOADED);

        return CampaignResponse.from(campaignRepository.save(campaign));
    }

    // 5. отправить на проверку
    public CampaignResponse submitForCheck(Long campaignId) {
        Campaign campaign = findCampaign(campaignId);
        requireStatus(campaign, CampaignStatus.CREATIVES_UPLOADED);

        campaign.setStatus(CampaignStatus.ON_MODERATION);

        return CampaignResponse.from(campaignRepository.save(campaign));
    }

    // Доработать по замечаниям модератора
    public CampaignResponse fixModerationIssues(Long campaignId, UploadCreativesRequest request) {
        Campaign campaign = findCampaign(campaignId);
        requireStatus(campaign, CampaignStatus.MODERATION_REJECTED);

        if (request.creatives() != null) {
            campaign.getCreatives().clear();
            List<Creative> creatives = request.creatives().stream()
                    .map(creativeRequest -> {
                        Creative creative = new Creative();
                        creative.setName(creativeRequest.name());
                        creative.setType(creativeRequest.type());
                        creative.setCampaign(campaign);
                        return creative;
                    })
                    .toList();
            campaign.getCreatives().addAll(creatives);
        }

        campaign.setStatus(CampaignStatus.CREATIVES_UPLOADED);

        return CampaignResponse.from(campaignRepository.save(campaign));
    }

    public CampaignResponse freezeCampaign(Long campaignId) {
        Campaign campaign = findCampaign(campaignId);
        requireStatus(campaign, CampaignStatus.ACTIVE);

        campaign.setStatus(CampaignStatus.FROZEN_NO_PAYMENT);

        return CampaignResponse.from(campaignRepository.save(campaign));
    }


    public CampaignResponse restartCampaign(Long campaignId, @Valid ProceedCampaignRequest request) {
        Campaign campaign = findCampaign(campaignId);
        requireStatus(campaign, CampaignStatus.FROZEN_NO_PAYMENT);

        if (request.proceed()) {
            campaign.setStatus(CampaignStatus.ACTIVE);
        } else {
            campaign.setStatus(CampaignStatus.STOPPED);
        }

        return CampaignResponse.from(campaignRepository.save(campaign));
    }

//    public CampaignResponse processModerationDecision(Long campaignId, ModerationDecisionRequest request) {
//        Campaign campaign = findCampaign(campaignId);
//        requireStatus(campaign, CampaignStatus.ON_MODERATION);
//
//        campaign.setModerationComment(request.comment());
//
//        if (Boolean.TRUE.equals(request.approved())) {
//            transition(
//                    campaign,
//                    CampaignStatus.ON_MODERATION,
//                    CampaignEventType.MODERATION_APPROVED,
//                    nonEmptyOrDefault(request.comment(), "Модерация одобрила кампанию")
//            );
//            issueInvoice(campaign, "Первичное формирование счёта после одобрения модерацией");
//            campaign.setStatus(CampaignStatus.WAITING_PAYMENT);
//        } else {
//            transition(
//                    campaign,
//                    CampaignStatus.MODERATION_REJECTED,
//                    CampaignEventType.MODERATION_REJECTED,
//                    nonEmptyOrDefault(request.comment(), "Модерация отклонила кампанию")
//            );
//        }
//
//        return CampaignResponse.from(campaignRepository.save(campaign));
//    }

//    private void issueInvoice(Campaign campaign, String details) {
//        int nextRevision = campaign.getInvoices().stream()
//                .map(Invoice::getRevision)
//                .max(Comparator.naturalOrder())
//                .orElse(0) + 1;
//
//        LocalDateTime now = LocalDateTime.now();
//
//        Invoice invoice = new Invoice();
//        invoice.setCampaign(campaign);
//        invoice.setRevision(nextRevision);
//        invoice.setStatus(InvoiceStatus.ISSUED);
//        invoice.setAmount(campaign.getBudgetAmount());
//        invoice.setIssuedAt(now);
//        invoice.setDueAt(now.plusDays(campaign.getInvoiceDueDays()));
//
//        campaign.getInvoices().add(invoice);
//        addHistory(campaign, CampaignEventType.INVOICE_CREATED, details);
//    }

    private Campaign findCampaign(Long campaignId) {
        return campaignRepository.findById(campaignId)
                .orElseThrow(() -> new NotFoundException("Кампания с id=" + campaignId + " не найдена"));
    }

    private void requireStatus(Campaign campaign, CampaignStatus... allowedStatuses) {
        if (Arrays.stream(allowedStatuses).noneMatch(status -> status == campaign.getStatus())) {
            throw new InvalidStateException(
                    "Некорректный переход из статуса " + campaign.getStatus() +
                            ". Ожидались: " + Arrays.toString(allowedStatuses)
            );
        }
    }

    private String nonEmptyOrDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
