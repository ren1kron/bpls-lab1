package ifmo.se.lab1app.client.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CampaignWorkflowService {
//
//    private static final int DEFAULT_INVOICE_DUE_DAYS = 7;
//    private static final Set<CampaignStatus> statuses = EnumSet.of(
//            CampaignStatus.WAITING_PAYMENT,
//            CampaignStatus.WAITING_START,
//            CampaignStatus.ACTIVE
//    );
//
//    private final CampaignRepository campaignRepository;
//    private final CampaignHistoryEventRepository historyEventRepository;
//
//    public CampaignWorkflowService(
//        CampaignRepository campaignRepository,
//        CampaignHistoryEventRepository historyEventRepository
//    ) {
//        this.campaignRepository = campaignRepository;
//        this.historyEventRepository = historyEventRepository;
//    }
//
//    public CampaignResponse createCampaign(DraftCampaignRequest request) {
//        Campaign campaign = new Campaign();
//        campaign.setName(request.name());
//        campaign.setObjective(request.objective());
//        campaign.setType(request.campaignType());
//        campaign.setStartMode(request.startMode());
//        campaign.setUrl(request.url());
//        campaign.setNotes(request.notes());
//        campaign.setStatus(CampaignStatus.DRAFT);
//
//        addHistory(campaign, CampaignEventType.CAMPAIGN_CREATED, "Черновик кампании создан");
//        addHistory(campaign, CampaignEventType.AUTO_VALIDATION_PASSED, "Автоматическая валидация черновика пройдена");
//
//        return CampaignResponse.from(campaignRepository.save(campaign));
//    }
//
//    public CampaignResponse updateCampaignDraft(Long campaignId, DraftCampaignRequest request) {
//        Campaign campaign = findCampaign(campaignId);
//        requireStatus(campaign, CampaignStatus.DRAFT);
//
//        campaign.setName(request.name());
//        campaign.setObjective(request.objective());
//        campaign.setType(request.campaignType());
//        campaign.setStartMode(request.startMode());
//        campaign.setUrl(request.url());
//        campaign.setNotes(request.notes());
//
//        addHistory(campaign, CampaignEventType.CAMPAIGN_DRAFT_UPDATED, "Черновик кампании обновлён");
//        addHistory(campaign, CampaignEventType.AUTO_VALIDATION_PASSED, "Автоматическая валидация черновика пройдена");
//
//        return CampaignResponse.from(campaignRepository.save(campaign));
//    }
//
//    @Transactional(readOnly = true)
//    public CampaignResponse getCampaign(Long campaignId) {
//        return CampaignResponse.from(findCampaign(campaignId));
//    }
//
//    @Transactional(readOnly = true)
//    public List<CampaignResponse> getCampaigns() {
//        return campaignRepository.findAll().stream()
//            .map(CampaignResponse::from)
//            .toList();
//    }
//
//    @Transactional(readOnly = true)
//    public List<CampaignHistoryEventResponse> getCampaignHistory(Long campaignId) {
//        findCampaign(campaignId);
//        return historyEventRepository.findByCampaignIdOrderByCreatedAtDesc(campaignId).stream()
//            .map(CampaignHistoryEventResponse::from)
//            .toList();
//    }
//
//    public CampaignResponse configureCampaign(Long campaignId, ConfigureCampaignRequest request) {
//        Campaign campaign = findCampaign(campaignId);
//        requireStatus(campaign, CampaignStatus.DRAFT);
//
//        campaign.setBudgetAmount(request.budgetAmount());
//        campaign.setStartAt(request.startAt());
//        campaign.setEndAt(request.endAt());
//        transition(
//            campaign,
//            CampaignStatus.CONFIGURED,
//            CampaignEventType.CAMPAIGN_CONFIGURED,
//            "Кампания настроена"
//        );
//
//        return CampaignResponse.from(campaignRepository.save(campaign));
//    }
//
//    public CampaignResponse uploadCreatives(Long campaignId, UploadCreativesRequest request) {
//        Campaign campaign = findCampaign(campaignId);
//        requireStatus(campaign, CampaignStatus.CONFIGURED, CampaignStatus.CREATIVES_UPLOADED);
//
//        campaign.getCreatives().clear();
//        List<Creative> creatives = request.creatives().stream()
//            .map(creativeRequest -> {
//                Creative creative = new Creative();
//                creative.setName(creativeRequest.name());
//                creative.setType(creativeRequest.type());
//                creative.setCampaign(campaign);
//                return creative;
//            })
//            .toList();
//        campaign.getCreatives().addAll(creatives);
//
//        transition(
//            campaign,
//            CampaignStatus.CREATIVES_UPLOADED,
//            CampaignEventType.CREATIVES_UPLOADED,
//            "Креативы загружены"
//        );
//
//        return CampaignResponse.from(campaignRepository.save(campaign));
//    }
//
//    public CampaignResponse submitForCheck(Long campaignId) {
//        Campaign campaign = findCampaign(campaignId);
//        requireStatus(campaign, CampaignStatus.CREATIVES_UPLOADED);
//
//        addHistory(campaign, CampaignEventType.SUBMITTED_FOR_CHECK, "Кампания отправлена на проверку");
//        transition(
//            campaign,
//            CampaignStatus.ON_MODERATION,
//            CampaignEventType.AUTO_VALIDATION_PASSED,
//            "Автоматическая валидация пройдена, кампания отправлена на модерацию"
//        );
//
//        return CampaignResponse.from(campaignRepository.save(campaign));
//    }
//
//    public CampaignResponse processModerationDecision(Long campaignId, ModerationDecisionRequest request) {
//        Campaign campaign = findCampaign(campaignId);
//        requireStatus(campaign, CampaignStatus.ON_MODERATION);
//
//        campaign.setModerationComment(request.comment());
//
//        if (Boolean.TRUE.equals(request.approved())) {
//            transition(
//                campaign,
//                CampaignStatus.ON_MODERATION,
//                CampaignEventType.MODERATION_APPROVED,
//                nonEmptyOrDefault(request.comment(), "Модерация одобрила кампанию")
//            );
//            issueInvoice(campaign, "Первичное формирование счёта после одобрения модерацией");
//            campaign.setStatus(CampaignStatus.WAITING_PAYMENT);
//        } else {
//            transition(
//                campaign,
//                CampaignStatus.MODERATION_REJECTED,
//                CampaignEventType.MODERATION_REJECTED,
//                nonEmptyOrDefault(request.comment(), "Модерация отклонила кампанию")
//            );
//        }
//
//        return CampaignResponse.from(campaignRepository.save(campaign));
//    }
//
//    public CampaignResponse fixModerationIssues(Long campaignId) {
//        Campaign campaign = findCampaign(campaignId);
//        requireStatus(campaign, CampaignStatus.MODERATION_REJECTED);
//
//        transition(
//            campaign,
//            CampaignStatus.CREATIVES_UPLOADED,
//            CampaignEventType.MODERATION_ISSUES_FIXED,
//            "Замечания модерации исправлены"
//        );
//
//        return CampaignResponse.from(campaignRepository.save(campaign));
//    }
//
//    public CampaignResponse markPaymentReceived(Long campaignId, PaymentReceivedRequest request) {
//        Campaign campaign = findCampaign(campaignId);
//        requireStatus(campaign, CampaignStatus.WAITING_PAYMENT);
//
//        Invoice invoice = getCurrentInvoice(campaign)
//            .orElseThrow(() -> new InvalidStateException("Для кампании не найден активный счёт"));
//        if (invoice.getStatus() != InvoiceStatus.ISSUED) {
//            throw new InvalidStateException("Оплату можно принять только для счёта в статусе ISSUED");
//        }
//
//        LocalDateTime paidAt = request.paidAt() == null ? LocalDateTime.now() : request.paidAt();
//        invoice.setStatus(InvoiceStatus.PAID);
//        invoice.setPaidAt(paidAt);
//        addHistory(campaign, CampaignEventType.INVOICE_PAID, "Счёт оплачен");
//
//        activateOrWaitStart(campaign, paidAt, "После оплаты счёта");
//
//        return CampaignResponse.from(campaignRepository.save(campaign));
//    }
//
//    public CampaignResponse markInvoiceDueDateReached(Long campaignId) {
//        Campaign campaign = findCampaign(campaignId);
//        requireStatus(campaign, CampaignStatus.WAITING_PAYMENT);
//
//        applyInvoiceOverdue(campaign, "Срок оплаты счёта истёк");
//
//        return CampaignResponse.from(campaignRepository.save(campaign));
//    }
//
//    public CampaignResponse requestPause(Long campaignId) {
//        Campaign campaign = findCampaign(campaignId);
//        requireStatus(campaign, CampaignStatus.ACTIVE);
//
//        addHistory(campaign, CampaignEventType.PAUSE_REQUESTED, "Получен запрос на паузу");
//        transition(campaign, CampaignStatus.PAUSED, CampaignEventType.CAMPAIGN_PAUSED, "Кампания поставлена на паузу");
//
//        return CampaignResponse.from(campaignRepository.save(campaign));
//    }
//
//    public CampaignResponse decideResumeOrStop(Long campaignId, ResumeDecisionRequest request) {
//        Campaign campaign = findCampaign(campaignId);
//        requireStatus(campaign, CampaignStatus.PAUSED, CampaignStatus.FROZEN_NO_PAYMENT);
//
//        if (!request.resume()) {
//            transition(campaign, CampaignStatus.STOPPED, CampaignEventType.CAMPAIGN_STOPPED, "Кампания остановлена менеджером");
//            return CampaignResponse.from(campaignRepository.save(campaign));
//        }
//
//        addHistory(campaign, CampaignEventType.RESUME_REQUESTED, "Запрошено возобновление кампании");
//        campaign.setBudgetFormed(request.budgetFormed());
//
//        boolean hasPaidInvoice = getCurrentInvoice(campaign)
//            .map(invoice -> invoice.getStatus() == InvoiceStatus.PAID)
//            .orElse(false);
//
//        if (request.budgetFormed() && hasPaidInvoice) {
//            addHistory(campaign, CampaignEventType.BUDGET_FORMED, "Бюджет подтверждён, кампания возобновляется");
//            transition(campaign, CampaignStatus.ACTIVE, CampaignEventType.CAMPAIGN_ACTIVATED, "Кампания снова активна");
//        } else {
//            addHistory(campaign, CampaignEventType.BUDGET_NOT_FORMED, "Недостаточно условий для возобновления, сформирован новый счёт");
//            issueInvoice(campaign, "Переформирование счёта при возобновлении");
//            campaign.setStatus(CampaignStatus.WAITING_PAYMENT);
//        }
//
//        return CampaignResponse.from(campaignRepository.save(campaign));
//    }
//
//    public CampaignResponse markBudgetExhausted(Long campaignId) {
//        Campaign campaign = findCampaign(campaignId);
//        requireStatus(campaign, CampaignStatus.ACTIVE);
//
//        transition(campaign, CampaignStatus.STOPPED, CampaignEventType.BUDGET_EXHAUSTED, "Кампания остановлена: бюджет исчерпан");
//
//        return CampaignResponse.from(campaignRepository.save(campaign));
//    }
//
//    public CampaignResponse markEndAtReached(Long campaignId) {
//        Campaign campaign = findCampaign(campaignId);
//        requireStatus(campaign, CampaignStatus.ACTIVE);
//
//        transition(campaign, CampaignStatus.STOPPED, CampaignEventType.END_AT_REACHED, "Кампания остановлена: наступил endAt");
//
//        return CampaignResponse.from(campaignRepository.save(campaign));
//    }
//
//    public CampaignResponse processTimersForCampaign(Long campaignId) {
//        Campaign campaign = findCampaign(campaignId);
//        processTimers(campaign, LocalDateTime.now());
//        return CampaignResponse.from(campaignRepository.save(campaign));
//    }
//
//    public void processTimersForAllCampaigns() {
//        List<Campaign> campaigns = campaignRepository.findByStatusIn(statuses);
//        LocalDateTime now = LocalDateTime.now();
//        for (Campaign campaign : campaigns) {
//            processTimers(campaign, now);
//        }
//        campaignRepository.saveAll(campaigns);
//    }
//
//    private void processTimers(Campaign campaign, LocalDateTime now) {
//        boolean changed = false;
//        if (campaign.getStatus() == CampaignStatus.WAITING_PAYMENT) {
//            Optional<Invoice> currentInvoice = getCurrentInvoice(campaign);
//            if (currentInvoice.isPresent()
//                && currentInvoice.get().getStatus() == InvoiceStatus.ISSUED
//                && !currentInvoice.get().getDueAt().isAfter(now)) {
//                applyInvoiceOverdue(campaign, "Сработал таймер срока оплаты счёта");
//                changed = true;
//            }
//        }
//
//        if (campaign.getStatus() == CampaignStatus.WAITING_START && !campaign.getStartAt().isAfter(now)) {
//            transition(campaign, CampaignStatus.ACTIVE, CampaignEventType.CAMPAIGN_ACTIVATED, "Сработал таймер startAt");
//            changed = true;
//        }
//
//        if (campaign.getStatus() == CampaignStatus.ACTIVE && !campaign.getEndAt().isAfter(now)) {
//            transition(campaign, CampaignStatus.STOPPED, CampaignEventType.END_AT_REACHED, "Сработал таймер endAt");
//            changed = true;
//        }
//
//        if (changed) {
//            addHistory(campaign, CampaignEventType.TIMERS_PROCESSED, "Таймеры процесса обработаны");
//        }
//    }
//
//    private void applyInvoiceOverdue(Campaign campaign, String details) {
//        Invoice invoice = getCurrentInvoice(campaign)
//            .orElseThrow(() -> new InvalidStateException("Для кампании не найден активный счёт"));
//
//        if (invoice.getStatus() != InvoiceStatus.ISSUED) {
//            throw new InvalidStateException("Срок оплаты можно фиксировать только для счёта в статусе ISSUED");
//        }
//
//        invoice.setStatus(InvoiceStatus.OVERDUE);
//        transition(campaign, CampaignStatus.FROZEN_NO_PAYMENT, CampaignEventType.CAMPAIGN_FROZEN, details);
//        addHistory(campaign, CampaignEventType.INVOICE_OVERDUE, "Счёт просрочен");
//    }
//
//    private void activateOrWaitStart(Campaign campaign, LocalDateTime now, String reason) {
//        if (campaign.getStartAt().isAfter(now)) {
//            transition(campaign, CampaignStatus.WAITING_START, CampaignEventType.WAITING_START, reason + ": ожидание startAt");
//            return;
//        }
//
//        transition(campaign, CampaignStatus.ACTIVE, CampaignEventType.CAMPAIGN_ACTIVATED, reason + ": кампания активирована");
//    }
//
//    private void issueInvoice(Campaign campaign, String details) {
//        int nextRevision = campaign.getInvoices().stream()
//            .map(Invoice::getRevision)
//            .max(Comparator.naturalOrder())
//            .orElse(0) + 1;
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
//
//    private Campaign findCampaign(Long campaignId) {
//        return campaignRepository.findById(campaignId)
//            .orElseThrow(() -> new NotFoundException("Кампания с id=" + campaignId + " не найдена"));
//    }
//
//    private Optional<Invoice> getCurrentInvoice(Campaign campaign) {
//        return campaign.getInvoices().stream()
//            .max(Comparator.comparingInt(Invoice::getRevision));
//    }
//
//    private void requireStatus(Campaign campaign, CampaignStatus... allowedStatuses) {
//        if (Arrays.stream(allowedStatuses).noneMatch(status -> status == campaign.getStatus())) {
//            throw new InvalidStateException(
//                "Некорректный переход из статуса " + campaign.getStatus() +
//                    ". Ожидались: " + Arrays.toString(allowedStatuses)
//            );
//        }
//    }
//
//    private void transition(Campaign campaign, CampaignStatus newStatus, CampaignEventType eventType, String details) {
//        campaign.setStatus(newStatus);
//        addHistory(campaign, eventType, details);
//    }
//
//    private void addHistory(Campaign campaign, CampaignEventType eventType, String details) {
//        CampaignHistoryEvent event = new CampaignHistoryEvent();
//        event.setCampaign(campaign);
//        event.setType(eventType);
//        event.setDetails(details);
//        campaign.getHistory().add(event);
//    }
//
//    private String nonEmptyOrDefault(String value, String defaultValue) {
//        return value == null || value.isBlank() ? defaultValue : value;
//    }
}
