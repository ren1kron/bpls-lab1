package ifmo.se.lab1app.shared.infra;

import ifmo.se.lab1app.shared.domain.Campaign;
import ifmo.se.lab1app.shared.domain.CampaignStatus;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CampaignRepository extends JpaRepository<Campaign, Long> {

    List<Campaign> findByStatusIn(Collection<CampaignStatus> statuses);

    Campaign findByPaymentId(String paymentId);

    List<Campaign> findByStatus(CampaignStatus status);
}
