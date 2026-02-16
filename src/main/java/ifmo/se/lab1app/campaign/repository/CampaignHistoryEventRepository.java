package ifmo.se.lab1app.campaign.repository;

import ifmo.se.lab1app.campaign.model.CampaignHistoryEvent;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CampaignHistoryEventRepository extends JpaRepository<CampaignHistoryEvent, Long> {

    List<CampaignHistoryEvent> findByCampaignIdOrderByCreatedAtDesc(Long campaignId);
}
