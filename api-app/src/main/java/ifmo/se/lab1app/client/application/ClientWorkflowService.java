package ifmo.se.lab1app.client.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import ifmo.se.lab1app.auth.application.CurrentUserService;
import ifmo.se.lab1app.client.api.dto.*;
import ifmo.se.lab1app.client.infra.CreativeRepository;
import ifmo.se.lab1app.exception.CreativeLimitExceededException;
import ifmo.se.lab1app.shared.application.TransactionExecutor;
import ifmo.se.lab1app.shared.domain.CreativeUploadStatus;
import ifmo.se.lab1app.shared.domain.CreativeUploadTask;
import ifmo.se.lab1app.shared.domain.KafkaOutboxEvent;
import ifmo.se.lab1app.shared.infra.CampaignRepository;
import ifmo.se.lab1app.exception.InvalidStateException;
import ifmo.se.lab1app.exception.NotFoundException;
import ifmo.se.lab1app.shared.domain.Campaign;
import ifmo.se.lab1app.shared.domain.CampaignStatus;
import ifmo.se.lab1app.shared.domain.UserRole;
import ifmo.se.lab1app.shared.infra.CreativeUploadTaskRepository;
import ifmo.se.lab1app.shared.infra.KafkaOutboxEventRepository;
import ifmo.se.lab1app.shared.kafka.CreativeUploadKafkaProperties;
import ifmo.se.lab1app.shared.kafka.dto.CreativeUploadRequestEvent;
import jakarta.validation.Valid;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class ClientWorkflowService {

    private static final int MAX_CREATIVES_PER_CAMPAIGN = 10;
    private static final Set<CreativeUploadStatus> ACTIVE_UPLOAD_STATUSES = EnumSet.of(
            CreativeUploadStatus.PENDING,
            CreativeUploadStatus.PROCESSING
    );

    private final CampaignRepository campaignRepository;
    private final CreativeRepository creativeRepository;
    private final CreativeUploadTaskRepository creativeUploadTaskRepository;
    private final KafkaOutboxEventRepository outboxRepository;
    private final TransactionExecutor transactions;
    private final CurrentUserService currentUserService;
    private final ObjectMapper objectMapper;
    private final CreativeUploadKafkaProperties kafkaProperties;

    public ClientWorkflowService(
            CampaignRepository campaignRepository,
            CreativeRepository creativeRepository,
            CreativeUploadTaskRepository creativeUploadTaskRepository,
            KafkaOutboxEventRepository outboxRepository,
            TransactionExecutor transactions,
            CurrentUserService currentUserService,
            ObjectMapper objectMapper,
            CreativeUploadKafkaProperties kafkaProperties
    ) {
        this.campaignRepository = campaignRepository;
        this.creativeRepository = creativeRepository;
        this.creativeUploadTaskRepository = creativeUploadTaskRepository;
        this.outboxRepository = outboxRepository;
        this.transactions = transactions;
        this.currentUserService = currentUserService;
        this.objectMapper = objectMapper;
        this.kafkaProperties = kafkaProperties;
    }

    // 1. создать черновик кампании
    @PreAuthorize("hasAuthority('campaign:create')")
    public CampaignResponse createDraft(DraftCampaignRequest request) {
        return transactions.write(() -> {
            Campaign campaign = new Campaign();
            campaign.setName(request.name());
            campaign.setObjective(request.objective());
            campaign.setType(request.campaignType());
            campaign.setStartMode(request.startMode());
            campaign.setUrl(request.url());
            campaign.setStatus(CampaignStatus.DRAFT);
            campaign.setOwner(currentUserService.requireCurrentUserAccount());

            return CampaignResponse.from(campaignRepository.save(campaign), List.of());
        });
    }

    // 1.1 обновить черновик кампании
    @PreAuthorize("hasAuthority('campaign:edit')")
    public CampaignResponse patchCampaignBasics(Long campaignId, UpdateDraftCampaignRequest request) {
        return transactions.write(() -> {
            Campaign campaign = findCampaign(campaignId);
            requireStatus(campaign, CampaignStatus.DRAFT, CampaignStatus.CONFIGURED, CampaignStatus.CREATIVES_UPLOADED, CampaignStatus.MODERATION_REJECTED);

            if (request.name() != null) {
                campaign.setName(request.name());
            }
            if (request.objective() != null) {
                campaign.setObjective(request.objective());
            }
            if (request.campaignType() != null) {
                campaign.setType(request.campaignType());
            }
            if (request.startMode() != null) {
                campaign.setStartMode(request.startMode());
            }
            if (request.url() != null) {
                campaign.setUrl(request.url());
            }

            return toResponse(campaignRepository.save(campaign));
        });
    }

    // 2. настроить кампанию (плейсменты/таргетинг/расписание/бюджет)
    @PreAuthorize("hasAuthority('campaign:configure')")
    public CampaignResponse configureCampaign(Long campaignId, ConfigureCampaignRequest request) {
        return transactions.write(() -> {
            Campaign campaign = findCampaign(campaignId);
            requireStatus(campaign, CampaignStatus.DRAFT);

            campaign.setBudgetAmount(request.budgetAmount());
            campaign.setRequestedStartAt(request.requestedStartAt());
            campaign.setDurationDays(request.durationDays());

            campaign.setStatus(CampaignStatus.CONFIGURED);

            return toResponse(campaignRepository.save(campaign));
        });
    }

    // 2.1. reconfigure campaign
    @PreAuthorize("hasAuthority('campaign:configure')")
    public CampaignResponse reconfigureCampaign(Long campaignId, ReconfigureCampaignRequest request) {
        return transactions.write(() -> {
            Campaign campaign = findCampaign(campaignId);
            requireStatus(campaign, CampaignStatus.CONFIGURED, CampaignStatus.CREATIVES_UPLOADED, CampaignStatus.MODERATION_REJECTED);

            if (request.budgetAmount() != null) {
                campaign.setBudgetAmount(request.budgetAmount());
            }
            if (request.requestedStartAt() != null) {
                campaign.setRequestedStartAt(request.requestedStartAt());
            }
            if (request.durationDays() != null) {
                campaign.setDurationDays(request.durationDays());
            }

            return toResponse(campaignRepository.save(campaign));
        });
    }

    // 3. Загрузить креатив (один)
    @PreAuthorize("hasAuthority('creative:manage')")
    public CreativeLoadTaskResponse addCreative(Long campaignId, CreativeRequest request) {
        return transactions.write(() -> {
            Campaign campaign = findCampaignForUpdate(campaignId);
            requireStatus(
                    campaign,
                    CampaignStatus.CONFIGURED,
                    CampaignStatus.CREATIVES_LOADING,
                    CampaignStatus.CREATIVES_UPLOADED,
                    CampaignStatus.MODERATION_REJECTED
            );

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
            task.setUrl(request.url());
            task.setType(request.type());
            task.setStatus(CreativeUploadStatus.PENDING);

            campaign.setStatus(CampaignStatus.CREATIVES_LOADING);
            CreativeUploadTask savedTask = creativeUploadTaskRepository.save(task);
            Campaign savedCampaign = campaignRepository.save(campaign);
            outboxRepository.save(requestEvent(savedTask));

            return CreativeLoadTaskResponse.from(savedTask, savedCampaign.getStatus());
        });
    }

    @PreAuthorize("hasAuthority('creative:manage')")
    public CreativeLoadTaskResponse getCreativeLoadTask(Long campaignId, String taskId) {
        return transactions.read(() -> {
            Campaign campaign = findCampaign(campaignId);
            CreativeUploadTask task = creativeUploadTaskRepository.findByIdAndCampaignId(taskId, campaign.getId())
                    .orElseThrow(() -> new NotFoundException("Creative upload task with id=" + taskId + " was never found"));
            return CreativeLoadTaskResponse.from(task, campaign.getStatus());
        });
    }

    // 3.1. Удалить креатив
    @PreAuthorize("hasAuthority('creative:manage')")
    public CampaignResponse deleteCreative(Long campaignId, Long creativeId) {
        return transactions.write(() -> {
            Campaign campaign = findCampaign(campaignId);
            requireStatus(campaign, CampaignStatus.CREATIVES_UPLOADED, CampaignStatus.MODERATION_REJECTED);

            creativeRepository.deleteByCampaignIdAndId(campaign.getId(), creativeId);

            if (creativeRepository.countByCampaignId(campaign.getId()) == 0) {
                campaign.setStatus(CampaignStatus.CONFIGURED);
            } else {
                campaign.setStatus(CampaignStatus.CREATIVES_UPLOADED);
            }

            return toResponse(campaignRepository.save(campaign));
        });
    }

    // 4. отправить на проверку
    @PreAuthorize("hasAuthority('campaign:submit')")
    public CampaignResponse submitForCheck(Long campaignId) {
        return transactions.write(() -> {
            Campaign campaign = findCampaign(campaignId);
            requireStatus(campaign, CampaignStatus.CREATIVES_UPLOADED, CampaignStatus.MODERATION_REJECTED);

            campaign.setStatus(CampaignStatus.ON_MODERATION);

            return toResponse(campaignRepository.save(campaign));
        });
    }

    // удалить кампанию
    @PreAuthorize("hasAuthority('campaign:delete')")
    public void deleteCampaign(Long campaignId) {
        transactions.write(() -> {
            Campaign campaign = findCampaign(campaignId);
            requireStatus(campaign, CampaignStatus.DRAFT, CampaignStatus.CONFIGURED, CampaignStatus.CREATIVES_UPLOADED, CampaignStatus.MODERATION_REJECTED);

            creativeRepository.deleteAllByCampaignId(campaign.getId());
            creativeUploadTaskRepository.deleteAllByCampaignId(campaign.getId());
            campaignRepository.delete(campaign);
        });
    }

    @PreAuthorize("hasAuthority('campaign:freeze')")
    public CampaignResponse freezeCampaign(Long campaignId) {
        return transactions.write(() -> {
            Campaign campaign = findCampaign(campaignId);
            requireStatus(campaign, CampaignStatus.ACTIVE);

            campaign.setStatus(CampaignStatus.FROZEN);

            return toResponse(campaignRepository.save(campaign));
        });
    }


    @PreAuthorize("hasAuthority('campaign:proceed')")
    public CampaignResponse restartCampaign(Long campaignId, @Valid ProceedCampaignRequest request) {
        return transactions.write(() -> {
            Campaign campaign = findCampaign(campaignId);
            requireStatus(campaign, CampaignStatus.FROZEN);

            if (request.proceed()) {
                campaign.setStatus(CampaignStatus.ACTIVE);
            } else {
                campaign.setStatus(CampaignStatus.STOPPED);
            }

            return toResponse(campaignRepository.save(campaign));
        });
    }

    private CampaignResponse toResponse(Campaign campaign) {
        if (campaign.getId() == null) {
            return CampaignResponse.from(campaign, List.of());
        }
        return CampaignResponse.from(campaign, creativeRepository.findAllByCampaignIdOrderByIdDesc(campaign.getId()));
    }

    private Campaign findCampaign(Long campaignId) {
        if (currentUserService.hasRole(UserRole.COMPANY_MODERATOR)) {
            return campaignRepository.findById(campaignId)
                    .orElseThrow(() -> new NotFoundException("Campaign with id=" + campaignId + " was never found"));
        }

        return campaignRepository.findByIdAndOwnerUsername(campaignId, currentUserService.requireAuthenticatedUser().username())
                .orElseThrow(() -> new AccessDeniedException("You can modify only your own campaigns"));
    }

    private Campaign findCampaignForUpdate(Long campaignId) {
        if (currentUserService.hasRole(UserRole.COMPANY_MODERATOR)) {
            return campaignRepository.findByIdForUpdate(campaignId)
                    .orElseThrow(() -> new NotFoundException("Campaign with id=" + campaignId + " was never found"));
        }

        return campaignRepository.findByIdAndOwnerUsernameForUpdate(
                        campaignId,
                        currentUserService.requireAuthenticatedUser().username()
                )
                .orElseThrow(() -> new AccessDeniedException("You can modify only your own campaigns"));
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
}
