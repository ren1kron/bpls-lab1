package ifmo.se.lab1app.shared.infra;

import ifmo.se.lab1app.shared.domain.EisOutboxEvent;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface EisOutboxEventRepository extends JpaRepository<EisOutboxEvent, Long> {

    Optional<EisOutboxEvent> findByEventKey(String eventKey);

    boolean existsByEventKey(String eventKey);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select event from EisOutboxEvent event where event.publishedAt is null order by event.id asc")
    List<EisOutboxEvent> findUnpublishedForUpdate(Pageable pageable);
}
