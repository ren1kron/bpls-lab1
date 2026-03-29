package ifmo.se.lab1app.shared.application;

import ifmo.se.lab1app.client.api.dto.CampaignResponse;
import ifmo.se.lab1app.exception.NotFoundException;
import ifmo.se.lab1app.shared.domain.Campaign;
import ifmo.se.lab1app.shared.infra.CampaignRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CommonActivitiesService {
    private final CampaignRepository campaignRepository;
    private final TransactionExecutor transactions;

    public CommonActivitiesService(CampaignRepository campaignRepo, TransactionExecutor transactions) {
        this.campaignRepository = campaignRepo;
        this.transactions = transactions;
    }

    public List<CampaignResponse> getCampaigns() {
        return transactions.read(() -> campaignRepository.findAll().stream()
                .map(CampaignResponse::from)
                .toList());
    }

    public CampaignResponse getCampaign(Long campaignId) {
        return transactions.read(() -> CampaignResponse.from(findCampaign(campaignId)));
    }

    private Campaign findCampaign(Long campaignId) {
        return campaignRepository.findById(campaignId)
                .orElseThrow(() -> new NotFoundException("Кампания с id=" + campaignId + " не найдена"));
    }
}
