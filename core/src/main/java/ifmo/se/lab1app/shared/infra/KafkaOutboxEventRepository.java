package ifmo.se.lab1app.shared.infra;

import ifmo.se.lab1app.shared.domain.KafkaOutboxEvent;
import jakarta.persistence.LockModeType;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface KafkaOutboxEventRepository extends JpaRepository<KafkaOutboxEvent, Long> {

    boolean existsByTopicAndEventKey(String topic, String eventKey);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select event from KafkaOutboxEvent event where event.publishedAt is null order by event.id asc")
    List<KafkaOutboxEvent> findUnpublishedForUpdate(Pageable pageable);
}
