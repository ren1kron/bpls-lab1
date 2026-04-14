package ifmo.se.lab1app.client.application;

import ifmo.se.lab1app.auth.application.CurrentUserService;
import ifmo.se.lab1app.client.api.dto.*;
import ifmo.se.lab1app.client.domain.creative.Creative;
import ifmo.se.lab1app.exception.CreativeLimitExceededException;
import ifmo.se.lab1app.shared.application.TransactionExecutor;
import ifmo.se.lab1app.shared.infra.CampaignRepository;
import ifmo.se.lab1app.exception.InvalidStateException;
import ifmo.se.lab1app.exception.NotFoundException;
import ifmo.se.lab1app.shared.domain.Campaign;
import ifmo.se.lab1app.shared.domain.CampaignStatus;
import ifmo.se.lab1app.shared.domain.UserRole;
import jakarta.validation.Valid;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class ClientWorkflowService {

    private static final int MAX_CREATIVES_PER_CAMPAIGN = 10;
    private final CampaignRepository campaignRepository;
    private final TransactionExecutor transactions;
    private final CurrentUserService currentUserService;

    public ClientWorkflowService(
            CampaignRepository campaignRepository,
            TransactionExecutor transactions,
            CurrentUserService currentUserService
    ) {
        this.campaignRepository = campaignRepository;
        this.transactions = transactions;
        this.currentUserService = currentUserService;
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

            return CampaignResponse.from(campaignRepository.save(campaign));
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

            return CampaignResponse.from(campaignRepository.save(campaign));
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

            return CampaignResponse.from(campaignRepository.save(campaign));
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

            return CampaignResponse.from(campaignRepository.save(campaign));
        });
    }

    // 3. Загрузить креатив (один)
    @PreAuthorize("hasAuthority('creative:manage')")
    public CampaignResponse addCreative(Long campaignId, CreativeRequest request) {
        return transactions.write(() -> {
            Campaign campaign = findCampaign(campaignId);
            requireStatus(campaign, CampaignStatus.CONFIGURED, CampaignStatus.CREATIVES_UPLOADED, CampaignStatus.MODERATION_REJECTED);

            if (campaign.getCreatives().size() >= MAX_CREATIVES_PER_CAMPAIGN) {
                throw new CreativeLimitExceededException(
                        "Prohibited to add more than " + MAX_CREATIVES_PER_CAMPAIGN + " creatives to one campaign."
                );
            }

            Creative creative = new Creative();
            creative.setCampaign(campaign);
            creative.setName(request.url());
            creative.setType(request.type());

            campaign.getCreatives().add(creative);
            campaign.setStatus(CampaignStatus.CREATIVES_UPLOADED);

            return CampaignResponse.from(campaignRepository.save(campaign));
        });
    }

    // 3.1. Удалить креатив
    @PreAuthorize("hasAuthority('creative:manage')")
    public CampaignResponse deleteCreative(Long campaignId, Long creativeId) {
        return transactions.write(() -> {
            Campaign campaign = findCampaign(campaignId);
            requireStatus(campaign, CampaignStatus.CREATIVES_UPLOADED, CampaignStatus.MODERATION_REJECTED);

            campaign.getCreatives().removeIf(
                    creative -> Objects.equals(creative.getId(), creativeId)
            );

            if (campaign.getCreatives().isEmpty()) {
                campaign.setStatus(CampaignStatus.CONFIGURED);
            } else {
                campaign.setStatus(CampaignStatus.CREATIVES_UPLOADED);
            }

            return CampaignResponse.from(campaignRepository.save(campaign));
        });
    }

    // 4. отправить на проверку
    @PreAuthorize("hasAuthority('campaign:submit')")
    public CampaignResponse submitForCheck(Long campaignId) {
        return transactions.write(() -> {
            Campaign campaign = findCampaign(campaignId);
            requireStatus(campaign, CampaignStatus.CREATIVES_UPLOADED, CampaignStatus.MODERATION_REJECTED);

            campaign.setStatus(CampaignStatus.ON_MODERATION);

            return CampaignResponse.from(campaignRepository.save(campaign));
        });
    }

    // удалить кампанию
    @PreAuthorize("hasAuthority('campaign:delete')")
    public void deleteCampaign(Long campaignId) {
        transactions.write(() -> {
            Campaign campaign = findCampaign(campaignId);
            requireStatus(campaign, CampaignStatus.DRAFT, CampaignStatus.CONFIGURED, CampaignStatus.CREATIVES_UPLOADED, CampaignStatus.MODERATION_REJECTED);

            campaignRepository.delete(campaign);
        });
    }

    @PreAuthorize("hasAuthority('campaign:freeze')")
    public CampaignResponse freezeCampaign(Long campaignId) {
        return transactions.write(() -> {
            Campaign campaign = findCampaign(campaignId);
            requireStatus(campaign, CampaignStatus.ACTIVE);

            campaign.setStatus(CampaignStatus.FROZEN);

            return CampaignResponse.from(campaignRepository.save(campaign));
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

            return CampaignResponse.from(campaignRepository.save(campaign));
        });
    }

    private Campaign findCampaign(Long campaignId) {
        if (currentUserService.hasRole(UserRole.COMPANY_MODERATOR)) {
            return campaignRepository.findById(campaignId)
                    .orElseThrow(() -> new NotFoundException("Campaign with id=" + campaignId + " was never found"));
        }

        return campaignRepository.findByIdAndOwnerUsername(campaignId, currentUserService.requireAuthenticatedUser().username())
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
}
