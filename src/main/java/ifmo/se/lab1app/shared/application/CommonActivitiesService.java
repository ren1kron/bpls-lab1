package ifmo.se.lab1app.shared.application;

import ifmo.se.lab1app.auth.application.CurrentUserService;
import ifmo.se.lab1app.client.api.dto.CampaignResponse;
import ifmo.se.lab1app.client.domain.creative.Creative;
import ifmo.se.lab1app.client.infra.CreativeRepository;
import ifmo.se.lab1app.exception.NotFoundException;
import ifmo.se.lab1app.shared.domain.Campaign;
import ifmo.se.lab1app.shared.domain.UserRole;
import ifmo.se.lab1app.shared.infra.CampaignRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CommonActivitiesService {
    private final CampaignRepository campaignRepository;
    private final CreativeRepository creativeRepository;
    private final TransactionExecutor transactions;
    private final CurrentUserService currentUserService;

    public CommonActivitiesService(
            CampaignRepository campaignRepo,
            CreativeRepository creativeRepository,
            TransactionExecutor transactions,
            CurrentUserService currentUserService
    ) {
        this.campaignRepository = campaignRepo;
        this.creativeRepository = creativeRepository;
        this.transactions = transactions;
        this.currentUserService = currentUserService;
    }

    @PreAuthorize("hasAuthority('campaign:view')")
    public List<CampaignResponse> getCampaigns() {
        return transactions.read(() -> {
            if (currentUserService.hasRole(UserRole.COMPANY_MODERATOR)) {
                return toResponses(campaignRepository.findAll());
            }

            String username = currentUserService.requireAuthenticatedUser().username();
            return toResponses(campaignRepository.findAllByOwnerUsername(username));
        });
    }

    @PreAuthorize("hasAuthority('campaign:view')")
    public CampaignResponse getCampaign(Long campaignId) {
        return transactions.read(() -> toResponse(findCampaign(campaignId)));
    }

    private List<CampaignResponse> toResponses(List<Campaign> campaigns) {
        Map<Long, List<Creative>> creativesByCampaign = findCreativesByCampaign(campaigns);
        return campaigns.stream()
                .map(campaign -> CampaignResponse.from(
                        campaign,
                        creativesByCampaign.getOrDefault(campaign.getId(), List.of())
                ))
                .toList();
    }

    private CampaignResponse toResponse(Campaign campaign) {
        return CampaignResponse.from(campaign, creativeRepository.findAllByCampaignIdOrderByIdDesc(campaign.getId()));
    }

    private Map<Long, List<Creative>> findCreativesByCampaign(Collection<Campaign> campaigns) {
        List<Long> campaignIds = campaigns.stream()
                .map(Campaign::getId)
                .toList();
        if (campaignIds.isEmpty()) {
            return Map.of();
        }
        return creativeRepository.findAllByCampaignIds(campaignIds).stream()
                .collect(Collectors.groupingBy(Creative::getCampaignId));
    }

    private Campaign findCampaign(Long campaignId) {
        if (currentUserService.hasRole(UserRole.COMPANY_MODERATOR)) {
            return campaignRepository.findById(campaignId)
                    .orElseThrow(() -> new NotFoundException("Кампания с id=" + campaignId + " не найдена"));
        }

        return campaignRepository.findByIdAndOwnerUsername(campaignId, currentUserService.requireAuthenticatedUser().username())
                .orElseThrow(() -> new AccessDeniedException("Кампания доступна только владельцу"));
    }
}
