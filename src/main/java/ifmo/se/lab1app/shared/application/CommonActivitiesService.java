package ifmo.se.lab1app.shared.application;

import ifmo.se.lab1app.client.api.dto.CampaignResponse;
import ifmo.se.lab1app.exception.NotFoundException;
import ifmo.se.lab1app.shared.domain.Campaign;
import ifmo.se.lab1app.shared.infra.CampaignRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class CommonActivitiesService {
    private final CampaignRepository campaignRepository;

    public CommonActivitiesService(CampaignRepository campaignRepo) {
        this.campaignRepository = campaignRepo;
    }

    public List<CampaignResponse> getCampaigns() {
        return campaignRepository.findAll().stream()
                .map(CampaignResponse::from)
                .toList();
    }

    public CampaignResponse getCampaign(Long campaignId) {
        return CampaignResponse.from(findCampaign(campaignId));
    }

    private Campaign findCampaign(Long campaignId) {
        return campaignRepository.findById(campaignId)
                .orElseThrow(() -> new NotFoundException("Кампания с id=" + campaignId + " не найдена"));
    }
}
