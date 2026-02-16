package ifmo.se.lab1app.campaign.repository;

import ifmo.se.lab1app.campaign.model.Invoice;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    Optional<Invoice> findTopByCampaignIdOrderByRevisionDesc(Long campaignId);

    List<Invoice> findByCampaignIdOrderByRevisionDesc(Long campaignId);
}
