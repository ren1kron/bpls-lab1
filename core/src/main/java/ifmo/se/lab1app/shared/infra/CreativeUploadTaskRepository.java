package ifmo.se.lab1app.shared.infra;

import ifmo.se.lab1app.shared.domain.CreativeUploadStatus;
import ifmo.se.lab1app.shared.domain.CreativeUploadTask;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CreativeUploadTaskRepository extends JpaRepository<CreativeUploadTask, String> {

    Optional<CreativeUploadTask> findByIdAndCampaignId(String id, Long campaignId);

    void deleteAllByCampaignId(Long campaignId);

    long countByCampaignIdAndStatusIn(Long campaignId, Collection<CreativeUploadStatus> statuses);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select task from CreativeUploadTask task where task.id = :id")
    Optional<CreativeUploadTask> findByIdForUpdate(@Param("id") String id);
}
