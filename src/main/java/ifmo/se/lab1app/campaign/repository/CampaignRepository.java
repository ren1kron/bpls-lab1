package ifmo.se.lab1app.campaign.repository;

import ifmo.se.lab1app.campaign.model.Campaign;
import ifmo.se.lab1app.campaign.model.CampaignStatus;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CampaignRepository extends JpaRepository<Campaign, Long> {

    List<Campaign> findByStatusIn(Collection<CampaignStatus> statuses);
}
