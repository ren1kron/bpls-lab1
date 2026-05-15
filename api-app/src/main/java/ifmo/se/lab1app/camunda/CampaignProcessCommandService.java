package ifmo.se.lab1app.camunda;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import ifmo.se.lab1app.auth.domain.UserAccount;
import ifmo.se.lab1app.auth.infra.UserAccountRepository;
import ifmo.se.lab1app.billing.yookassa.application.YooKassaPaymentClient;
import ifmo.se.lab1app.billing.yookassa.application.YooKassaPaymentResult;
import ifmo.se.lab1app.client.domain.creative.CreativeType;
import ifmo.se.lab1app.client.domain.enums.CampaignObjective;
import ifmo.se.lab1app.client.domain.enums.CampaignType;
import ifmo.se.lab1app.client.domain.enums.StartMode;
import ifmo.se.lab1app.client.infra.CreativeRepository;
import ifmo.se.lab1app.eis.CampaignEisEventPublisher;
import ifmo.se.lab1app.exception.CreativeLimitExceededException;
import ifmo.se.lab1app.exception.InvalidStateException;
import ifmo.se.lab1app.exception.NotFoundException;
import ifmo.se.lab1app.shared.application.TransactionExecutor;
import ifmo.se.lab1app.shared.domain.Campaign;
import ifmo.se.lab1app.shared.domain.CampaignStatus;
import ifmo.se.lab1app.shared.domain.CreativeUploadStatus;
import ifmo.se.lab1app.shared.domain.CreativeUploadTask;
import ifmo.se.lab1app.shared.domain.KafkaOutboxEvent;
import ifmo.se.lab1app.shared.infra.CampaignRepository;
import ifmo.se.lab1app.shared.infra.CreativeUploadTaskRepository;
import ifmo.se.lab1app.shared.infra.KafkaOutboxEventRepository;
import ifmo.se.lab1app.shared.kafka.CreativeUploadKafkaProperties;
import ifmo.se.lab1app.shared.kafka.dto.CreativeUploadRequestEvent;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class CampaignProcessCommandService {

    private static final int MAX_CREATIVES_PER_CAMPAIGN = 10;
    private static final Set<CreativeUploadStatus> ACTIVE_UPLOAD_STATUSES = EnumSet.of(
            CreativeUploadStatus.PENDING,
            CreativeUploadStatus.PROCESSING
    );

    private final CampaignRepository campaignRepository;
    private final CreativeRepository creativeRepository;
    private final CreativeUploadTaskRepository creativeUploadTaskRepository;
    private final KafkaOutboxEventRepository outboxRepository;
    private final UserAccountRepository userAccountRepository;
    private final TransactionExecutor transactions;
    private final ObjectMapper objectMapper;
    private final CreativeUploadKafkaProperties kafkaProperties;
    private final CampaignEisEventPublisher eisEventPublisher;
    private final YooKassaPaymentClient yooKassaPaymentClient;

    public CampaignProcessCommandService(
            CampaignRepository campaignRepository,
            CreativeRepository creativeRepository,
            CreativeUploadTaskRepository creativeUploadTaskRepository,
            KafkaOutboxEventRepository outboxRepository,
            UserAccountRepository userAccountRepository,
            TransactionExecutor transactions,
            ObjectMapper objectMapper,
            CreativeUploadKafkaProperties kafkaProperties,
            CampaignEisEventPublisher eisEventPublisher,
            YooKassaPaymentClient yooKassaPaymentClient
    ) {
        this.campaignRepository = campaignRepository;
        this.creativeRepository = creativeRepository;
        this.creativeUploadTaskRepository = creativeUploadTaskRepository;
        this.outboxRepository = outboxRepository;
        this.userAccountRepository = userAccountRepository;
        this.transactions = transactions;
        this.objectMapper = objectMapper;
        this.kafkaProperties = kafkaProperties;
        this.eisEventPublisher = eisEventPublisher;
        this.yooKassaPaymentClient = yooKassaPaymentClient;
    }

    public Campaign createDraft(Map<String, Object> variables) {
        return transactions.write(() -> {
            UserAccount owner = user(stringValue(variables, "ownerUsername"));
            Campaign campaign = new Campaign();
            campaign.setName(requiredString(variables, "name"));
            campaign.setObjective(enumValue(variables, "objective", CampaignObjective.class));
            campaign.setType(enumValue(variables, "campaignType", CampaignType.class));
            campaign.setStartMode(enumValue(variables, "startMode", StartMode.class));
            campaign.setUrl(requiredString(variables, "url"));
            campaign.setStatus(CampaignStatus.DRAFT);
            campaign.setOwner(owner);

            Campaign savedCampaign = campaignRepository.save(campaign);
            eisEventPublisher.publish("CampaignCreated", null, savedCampaign.getStatus(), savedCampaign, owner);
            return savedCampaign;
        });
    }

    public Campaign updateBasics(Long campaignId, Map<String, Object> variables) {
        return transactions.write(() -> {
            Campaign campaign = findCampaign(campaignId);
            requireStatus(campaign, CampaignStatus.DRAFT, CampaignStatus.CONFIGURED,
                    CampaignStatus.CREATIVES_UPLOADED, CampaignStatus.MODERATION_REJECTED);
            CampaignStatus statusBefore = campaign.getStatus();

            optionalString(variables, "name").ifPresent(campaign::setName);
            optionalEnum(variables, "objective", CampaignObjective.class).ifPresent(campaign::setObjective);
            optionalEnum(variables, "campaignType", CampaignType.class).ifPresent(campaign::setType);
            optionalEnum(variables, "startMode", StartMode.class).ifPresent(campaign::setStartMode);
            optionalString(variables, "url").ifPresent(campaign::setUrl);

            Campaign savedCampaign = campaignRepository.save(campaign);
            eisEventPublisher.publish("CampaignUpdated", statusBefore, savedCampaign.getStatus(),
                    savedCampaign, operationUser(savedCampaign, variables));
            return savedCampaign;
        });
    }

    public Campaign configure(Long campaignId, Map<String, Object> variables) {
        return transactions.write(() -> {
            Campaign campaign = findCampaign(campaignId);
            requireStatus(campaign, CampaignStatus.DRAFT);
            applyConfiguration(campaign, variables, true);

            CampaignStatus statusBefore = campaign.getStatus();
            campaign.setStatus(CampaignStatus.CONFIGURED);
            Campaign savedCampaign = campaignRepository.save(campaign);
            eisEventPublisher.publish("CampaignConfigured", statusBefore, savedCampaign.getStatus(),
                    savedCampaign, operationUser(savedCampaign, variables));
            return savedCampaign;
        });
    }

    public Campaign reconfigure(Long campaignId, Map<String, Object> variables) {
        return transactions.write(() -> {
            Campaign campaign = findCampaign(campaignId);
            requireStatus(campaign, CampaignStatus.CONFIGURED, CampaignStatus.CREATIVES_UPLOADED,
                    CampaignStatus.MODERATION_REJECTED);
            CampaignStatus statusBefore = campaign.getStatus();
            applyConfiguration(campaign, variables, false);

            Campaign savedCampaign = campaignRepository.save(campaign);
            eisEventPublisher.publish("CampaignUpdated", statusBefore, savedCampaign.getStatus(),
                    savedCampaign, operationUser(savedCampaign, variables));
            return savedCampaign;
        });
    }

    public CreativeUploadTask scheduleCreativeUpload(Long campaignId, Map<String, Object> variables) {
        return transactions.write(() -> {
            Campaign campaign = findCampaignForUpdate(campaignId);
            requireStatus(campaign, CampaignStatus.CONFIGURED, CampaignStatus.CREATIVES_LOADING,
                    CampaignStatus.CREATIVES_UPLOADED, CampaignStatus.MODERATION_REJECTED);

            long activeTasks = creativeUploadTaskRepository.countByCampaignIdAndStatusIn(
                    campaign.getId(),
                    ACTIVE_UPLOAD_STATUSES
            );
            long totalScheduledCreatives = creativeRepository.countByCampaignId(campaign.getId()) + activeTasks;
            if (totalScheduledCreatives >= MAX_CREATIVES_PER_CAMPAIGN) {
                throw new CreativeLimitExceededException(
                        "Prohibited to add more than " + MAX_CREATIVES_PER_CAMPAIGN + " creatives to one campaign."
                );
            }

            CreativeUploadTask task = new CreativeUploadTask();
            task.setId(UUID.randomUUID().toString());
            task.setCampaignId(campaign.getId());
            task.setUrl(requiredString(variables, "creativeUrl"));
            task.setType(enumValue(variables, "creativeType", CreativeType.class));
            task.setStatus(CreativeUploadStatus.PENDING);

            campaign.setStatus(CampaignStatus.CREATIVES_LOADING);
            CreativeUploadTask savedTask = creativeUploadTaskRepository.save(task);
            campaignRepository.save(campaign);
            outboxRepository.save(requestEvent(savedTask));
            return savedTask;
        });
    }

    public Campaign deleteCreative(Long campaignId, Map<String, Object> variables) {
        return transactions.write(() -> {
            Campaign campaign = findCampaign(campaignId);
            requireStatus(campaign, CampaignStatus.CREATIVES_UPLOADED, CampaignStatus.MODERATION_REJECTED);
            CampaignStatus statusBefore = campaign.getStatus();
            Long creativeId = longValue(variables, "creativeId");
            if (creativeId == null) {
                return campaign;
            }

            creativeRepository.deleteByCampaignIdAndId(campaign.getId(), creativeId);
            campaign.setStatus(creativeRepository.countByCampaignId(campaign.getId()) == 0
                    ? CampaignStatus.CONFIGURED
                    : CampaignStatus.CREATIVES_UPLOADED);
            Campaign savedCampaign = campaignRepository.save(campaign);
            eisEventPublisher.publish("CreativeDeleted", statusBefore, savedCampaign.getStatus(),
                    savedCampaign, operationUser(savedCampaign, variables));
            return savedCampaign;
        });
    }

    public Campaign submitForModeration(Long campaignId, Map<String, Object> variables) {
        return transactions.write(() -> {
            Campaign campaign = findCampaign(campaignId);
            requireStatus(campaign, CampaignStatus.CREATIVES_UPLOADED, CampaignStatus.MODERATION_REJECTED);
            CampaignStatus statusBefore = campaign.getStatus();
            campaign.setStatus(CampaignStatus.ON_MODERATION);
            Campaign savedCampaign = campaignRepository.save(campaign);
            eisEventPublisher.publish("CampaignSubmittedToModeration", statusBefore, savedCampaign.getStatus(),
                    savedCampaign, operationUser(savedCampaign, variables));
            return savedCampaign;
        });
    }

    public Campaign processModerationDecision(Long campaignId, Map<String, Object> variables) {
        return transactions.write(() -> {
            Campaign campaign = findCampaign(campaignId);
            requireStatus(campaign, CampaignStatus.ON_MODERATION);
            CampaignStatus statusBefore = campaign.getStatus();
            String comment = stringValue(variables, "moderationComment");
            campaign.setModerationComment(comment);

            if (booleanValue(variables, "moderationApproved")) {
                YooKassaPaymentResult payment = yooKassaPaymentClient.createPayment(campaign);
                campaign.setPaymentId(payment.id());
                campaign.setPaymentConfirmationUrl(payment.confirmationUrl());
                campaign.setStatus(CampaignStatus.WAITING_PAYMENT);
            } else {
                campaign.setPaymentId(null);
                campaign.setPaymentConfirmationUrl(null);
                campaign.setStatus(CampaignStatus.MODERATION_REJECTED);
            }

            Campaign savedCampaign = campaignRepository.save(campaign);
            UserAccount operationUser = operationUser(savedCampaign, variables);
            if (booleanValue(variables, "moderationApproved")) {
                eisEventPublisher.publish("ModerationApproved", statusBefore, savedCampaign.getStatus(),
                        comment, savedCampaign, operationUser);
                eisEventPublisher.publish("PaymentInvoiceIssued", statusBefore, savedCampaign.getStatus(),
                        "invoice issued", savedCampaign, operationUser);
            } else {
                eisEventPublisher.publish("ModerationRejected", statusBefore, savedCampaign.getStatus(),
                        comment, savedCampaign, operationUser);
            }
            return savedCampaign;
        });
    }

    public Campaign markPaymentSucceeded(Long campaignId) {
        return transactions.write(() -> {
            Campaign campaign = findCampaign(campaignId);
            requireStatus(campaign, CampaignStatus.WAITING_PAYMENT);
            CampaignStatus statusBefore = campaign.getStatus();
            campaign.setStatus(CampaignStatus.WAITING_START);
            Campaign savedCampaign = campaignRepository.save(campaign);
            eisEventPublisher.publish("PaymentSucceeded", statusBefore, savedCampaign.getStatus(),
                    "payment.succeeded", savedCampaign);
            return savedCampaign;
        });
    }

    public Campaign markPaymentCanceled(Long campaignId) {
        return transactions.write(() -> {
            Campaign campaign = findCampaign(campaignId);
            requireStatus(campaign, CampaignStatus.WAITING_PAYMENT);
            CampaignStatus statusBefore = campaign.getStatus();
            campaign.setStatus(CampaignStatus.FROZEN);
            Campaign savedCampaign = campaignRepository.save(campaign);
            eisEventPublisher.publish("PaymentCanceled", statusBefore, savedCampaign.getStatus(),
                    "payment.canceled", savedCampaign);
            return savedCampaign;
        });
    }

    public Campaign retryPayment(Long campaignId, Map<String, Object> variables) {
        return transactions.write(() -> {
            Campaign campaign = findCampaign(campaignId);
            requireStatus(campaign, CampaignStatus.FROZEN);
            CampaignStatus statusBefore = campaign.getStatus();
            YooKassaPaymentResult payment = yooKassaPaymentClient.createPayment(campaign);
            campaign.setPaymentId(payment.id());
            campaign.setPaymentConfirmationUrl(payment.confirmationUrl());
            campaign.setStatus(CampaignStatus.WAITING_PAYMENT);
            Campaign savedCampaign = campaignRepository.save(campaign);
            eisEventPublisher.publish("PaymentInvoiceIssued", statusBefore, savedCampaign.getStatus(),
                    "invoice reissued", savedCampaign, operationUser(savedCampaign, variables));
            return savedCampaign;
        });
    }

    public Campaign activate(Long campaignId) {
        return transactions.write(() -> {
            Campaign campaign = findCampaign(campaignId);
            requireStatus(campaign, CampaignStatus.WAITING_START);
            CampaignStatus statusBefore = campaign.getStatus();
            LocalDateTime now = LocalDateTime.now();
            campaign.setActualStartAt(now);
            campaign.setActualEndAt(now.plusDays(campaign.getDurationDays()));
            campaign.setStatus(CampaignStatus.ACTIVE);
            Campaign savedCampaign = campaignRepository.save(campaign);
            eisEventPublisher.publish("CampaignStarted", statusBefore, savedCampaign.getStatus(), savedCampaign);
            return savedCampaign;
        });
    }

    public Campaign freezeActive(Long campaignId, Map<String, Object> variables) {
        return transactions.write(() -> {
            Campaign campaign = findCampaign(campaignId);
            requireStatus(campaign, CampaignStatus.ACTIVE);
            CampaignStatus statusBefore = campaign.getStatus();
            campaign.setStatus(CampaignStatus.FROZEN);
            campaign.setFrozenAt(LocalDateTime.now());
            Campaign savedCampaign = campaignRepository.save(campaign);
            eisEventPublisher.publish("CampaignFrozen", statusBefore, savedCampaign.getStatus(),
                    savedCampaign, operationUser(savedCampaign, variables));
            return savedCampaign;
        });
    }

    public Campaign resume(Long campaignId, Map<String, Object> variables) {
        return transactions.write(() -> {
            Campaign campaign = findCampaign(campaignId);
            requireStatus(campaign, CampaignStatus.FROZEN);
            CampaignStatus statusBefore = campaign.getStatus();
            campaign.setStatus(CampaignStatus.ACTIVE);
            Campaign savedCampaign = campaignRepository.save(campaign);
            eisEventPublisher.publish("CampaignResumed", statusBefore, savedCampaign.getStatus(),
                    savedCampaign, operationUser(savedCampaign, variables));
            return savedCampaign;
        });
    }

    public Campaign stop(Long campaignId, Map<String, Object> variables) {
        return transactions.write(() -> {
            Campaign campaign = findCampaign(campaignId);
            requireStatus(campaign, CampaignStatus.ACTIVE, CampaignStatus.FROZEN, CampaignStatus.WAITING_START);
            CampaignStatus statusBefore = campaign.getStatus();
            campaign.setStatus(CampaignStatus.STOPPED);
            Campaign savedCampaign = campaignRepository.save(campaign);
            String eventType = statusBefore == CampaignStatus.FROZEN ? "CampaignFinishedByClient" : "CampaignStopped";
            eisEventPublisher.publish(eventType, statusBefore, savedCampaign.getStatus(),
                    savedCampaign, operationUser(savedCampaign, variables));
            return savedCampaign;
        });
    }

    public void deleteCampaign(Long campaignId, Map<String, Object> variables) {
        transactions.write(() -> {
            Campaign campaign = findCampaign(campaignId);
            requireStatus(campaign, CampaignStatus.DRAFT, CampaignStatus.CONFIGURED,
                    CampaignStatus.CREATIVES_UPLOADED, CampaignStatus.MODERATION_REJECTED);
            creativeRepository.deleteAllByCampaignId(campaign.getId());
            creativeUploadTaskRepository.deleteAllByCampaignId(campaign.getId());
            eisEventPublisher.publish("CampaignDeleted", campaign.getStatus(), null,
                    "deleted before launch", campaign, operationUser(campaign, variables));
            campaignRepository.delete(campaign);
        });
    }

    private void applyConfiguration(Campaign campaign, Map<String, Object> variables, boolean required) {
        BigDecimal budgetAmount = decimalValue(variables, "budgetAmount");
        LocalDateTime requestedStartAt = dateTimeValue(variables, "requestedStartAt");
        Integer durationDays = intValue(variables, "durationDays");
        if (required || budgetAmount != null) {
            campaign.setBudgetAmount(budgetAmount);
        }
        if (required || requestedStartAt != null) {
            campaign.setRequestedStartAt(requestedStartAt);
        }
        if (required || durationDays != null) {
            campaign.setDurationDays(durationDays);
        }
    }

    private Campaign findCampaign(Long campaignId) {
        return campaignRepository.findById(campaignId)
                .orElseThrow(() -> new NotFoundException("Campaign with id=" + campaignId + " was never found"));
    }

    private Campaign findCampaignForUpdate(Long campaignId) {
        return campaignRepository.findByIdForUpdate(campaignId)
                .orElseThrow(() -> new NotFoundException("Campaign with id=" + campaignId + " was never found"));
    }

    private UserAccount user(String username) {
        if (!StringUtils.hasText(username)) {
            throw new InvalidStateException("ownerUsername process variable is required");
        }
        return userAccountRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("User with username=" + username + " was never found"));
    }

    private UserAccount operationUser(Campaign campaign, Map<String, Object> variables) {
        String username = stringValue(variables, "operationUsername");
        if (!StringUtils.hasText(username)) {
            username = stringValue(variables, "moderatorUsername");
        }
        if (!StringUtils.hasText(username)) {
            username = stringValue(variables, "ownerUsername");
        }
        if (StringUtils.hasText(username)) {
            return userAccountRepository.findByUsername(username).orElse(campaign.getOwner());
        }
        return campaign.getOwner();
    }

    private void requireStatus(Campaign campaign, CampaignStatus... allowedStatuses) {
        if (Arrays.stream(allowedStatuses).noneMatch(status -> status == campaign.getStatus())) {
            throw new InvalidStateException(
                    "Incorrect move from status " + campaign.getStatus() +
                            ". Allowed: " + Arrays.toString(allowedStatuses)
            );
        }
    }

    private KafkaOutboxEvent requestEvent(CreativeUploadTask task) {
        CreativeUploadRequestEvent event = new CreativeUploadRequestEvent(
                task.getId(),
                task.getCampaignId(),
                task.getUrl(),
                task.getType()
        );
        try {
            return KafkaOutboxEvent.unpublished(
                    kafkaProperties.getUploadRequestTopic(),
                    task.getId(),
                    objectMapper.writeValueAsString(event)
            );
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize creative upload request event", exception);
        }
    }

    private String requiredString(Map<String, Object> variables, String name) {
        String value = stringValue(variables, name);
        if (!StringUtils.hasText(value)) {
            throw new InvalidStateException("Process variable " + name + " is required");
        }
        return value;
    }

    private java.util.Optional<String> optionalString(Map<String, Object> variables, String name) {
        String value = stringValue(variables, name);
        return StringUtils.hasText(value) ? java.util.Optional.of(value) : java.util.Optional.empty();
    }

    private String stringValue(Map<String, Object> variables, String name) {
        Object value = variables.get(name);
        return value == null ? null : value.toString();
    }

    private boolean booleanValue(Map<String, Object> variables, String name) {
        Object value = variables.get(name);
        if (value instanceof Boolean bool) {
            return bool;
        }
        return Boolean.parseBoolean(value == null ? "false" : value.toString());
    }

    private Integer intValue(Map<String, Object> variables, String name) {
        Long value = longValue(variables, name);
        return value == null ? null : value.intValue();
    }

    private Long longValue(Map<String, Object> variables, String name) {
        Object value = variables.get(name);
        if (value == null || !StringUtils.hasText(value.toString())) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(value.toString());
    }

    private BigDecimal decimalValue(Map<String, Object> variables, String name) {
        Object value = variables.get(name);
        if (value == null || !StringUtils.hasText(value.toString())) {
            return null;
        }
        return new BigDecimal(value.toString());
    }

    private LocalDateTime dateTimeValue(Map<String, Object> variables, String name) {
        Object value = variables.get(name);
        if (value == null || !StringUtils.hasText(value.toString())) {
            return null;
        }
        String text = value.toString();
        try {
            return LocalDateTime.parse(text);
        } catch (Exception ignored) {
            try {
                return OffsetDateTime.parse(text).toLocalDateTime();
            } catch (Exception ignoredAgain) {
                return LocalDate.parse(text).atStartOfDay();
            }
        }
    }

    private <E extends Enum<E>> java.util.Optional<E> optionalEnum(
            Map<String, Object> variables,
            String name,
            Class<E> enumType
    ) {
        String value = stringValue(variables, name);
        return StringUtils.hasText(value)
                ? java.util.Optional.of(Enum.valueOf(enumType, value))
                : java.util.Optional.empty();
    }

    private <E extends Enum<E>> E enumValue(Map<String, Object> variables, String name, Class<E> enumType) {
        return optionalEnum(variables, name, enumType)
                .orElseThrow(() -> new InvalidStateException("Process variable " + name + " is required"));
    }
}
