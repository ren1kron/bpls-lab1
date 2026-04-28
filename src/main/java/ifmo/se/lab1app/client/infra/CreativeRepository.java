package ifmo.se.lab1app.client.infra;

import ifmo.se.lab1app.client.domain.creative.Creative;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CreativeRepository extends JpaRepository<Creative, Long> {

    List<Creative> findAllByCampaignIdOrderByIdDesc(Long campaignId);

    long countByCampaignId(Long campaignId);

    Optional<Creative> findByUploadTaskId(String uploadTaskId);

    long deleteByCampaignIdAndId(Long campaignId, Long id);

    void deleteAllByCampaignId(Long campaignId);

    @Query("""
            select creative
            from Creative creative
            where creative.campaignId in :campaignIds
            order by creative.campaignId asc, creative.id desc
            """)
    List<Creative> findAllByCampaignIds(@Param("campaignIds") Collection<Long> campaignIds);
}
