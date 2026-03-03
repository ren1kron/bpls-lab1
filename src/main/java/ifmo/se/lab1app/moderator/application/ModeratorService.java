package ifmo.se.lab1app.moderator.application;

import ifmo.se.lab1app.client.api.dto.CampaignResponse;
import ifmo.se.lab1app.exception.InvalidStateException;
import ifmo.se.lab1app.exception.NotFoundException;
import ifmo.se.lab1app.moderator.api.dto.ModerationDecisionRequest;
import ifmo.se.lab1app.shared.domain.Campaign;
import ifmo.se.lab1app.shared.domain.CampaignStatus;
import ifmo.se.lab1app.shared.infra.CampaignRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;

@Slf4j
@Service
@Transactional
public class ModeratorService {

    private final CampaignRepository campaignRepository;

    public ModeratorService(CampaignRepository campaignRepository) {
        this.campaignRepository = campaignRepository;
    }

    public CampaignResponse processModerationDecision(Long campaignId, ModerationDecisionRequest request) {
        Campaign campaign = findCampaign(campaignId);
        requireStatus(campaign, CampaignStatus.ON_MODERATION);

        campaign.setModerationComment(request.comment());

        if (Boolean.TRUE.equals(request.approved())) {
            // i need to send this curl here
        } else {
            campaign.setStatus(CampaignStatus.MODERATION_REJECTED);
        }

        return CampaignResponse.from(campaignRepository.save(campaign));
    }

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
}
