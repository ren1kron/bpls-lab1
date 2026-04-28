package ifmo.se.lab1app.shared.infra;

import ifmo.se.lab1app.shared.domain.Campaign;
import ifmo.se.lab1app.shared.domain.CampaignStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CampaignRepository extends JpaRepository<Campaign, Long> {

    List<Campaign> findByStatusIn(Collection<CampaignStatus> statuses);

    Campaign findByPaymentId(String paymentId);

    List<Campaign> findByStatus(CampaignStatus status);

    List<Campaign> findAllByOwnerUsername(String username);

    Optional<Campaign> findByIdAndOwnerUsername(Long id, String username);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select campaign from Campaign campaign where campaign.id = :id")
    Optional<Campaign> findByIdForUpdate(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select campaign
            from Campaign campaign
            join campaign.owner owner
            where campaign.id = :id
              and owner.username = :username
            """)
    Optional<Campaign> findByIdAndOwnerUsernameForUpdate(
            @Param("id") Long id,
            @Param("username") String username
    );
}
