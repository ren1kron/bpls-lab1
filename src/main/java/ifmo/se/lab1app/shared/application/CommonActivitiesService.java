package ifmo.se.lab1app.shared.application;

import ifmo.se.lab1app.auth.application.CurrentUserService;
import ifmo.se.lab1app.client.api.dto.CampaignResponse;
import ifmo.se.lab1app.exception.NotFoundException;
import ifmo.se.lab1app.shared.domain.Campaign;
import ifmo.se.lab1app.shared.domain.UserRole;
import ifmo.se.lab1app.shared.infra.CampaignRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CommonActivitiesService {
    private final CampaignRepository campaignRepository;
    private final TransactionExecutor transactions;
    private final CurrentUserService currentUserService;

    public CommonActivitiesService(
            CampaignRepository campaignRepo,
            TransactionExecutor transactions,
            CurrentUserService currentUserService
    ) {
        this.campaignRepository = campaignRepo;
        this.transactions = transactions;
        this.currentUserService = currentUserService;
    }

    @PreAuthorize("hasAuthority('campaign:view')")
    public List<CampaignResponse> getCampaigns() {
        return transactions.read(() -> {
            if (currentUserService.hasRole(UserRole.COMPANY_MODERATOR)) {
                return campaignRepository.findAll().stream()
                        .map(CampaignResponse::from)
                        .toList();
            }

            String username = currentUserService.requireAuthenticatedUser().username();
            return campaignRepository.findAllByOwnerUsername(username).stream()
                    .map(CampaignResponse::from)
                    .toList();
        });
    }

    @PreAuthorize("hasAuthority('campaign:view')")
    public CampaignResponse getCampaign(Long campaignId) {
        return transactions.read(() -> CampaignResponse.from(findCampaign(campaignId)));
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
