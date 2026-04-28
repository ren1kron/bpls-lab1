package ifmo.se.lab1app.shared.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "kafka_outbox_events")
public class KafkaOutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String topic;

    @Column(name = "event_key", nullable = false)
    private String eventKey;

    @Column(nullable = false, columnDefinition = "text")
    private String payload;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public static KafkaOutboxEvent unpublished(String topic, String eventKey, String payload) {
        KafkaOutboxEvent event = new KafkaOutboxEvent();
        event.setTopic(topic);
        event.setEventKey(eventKey);
        event.setPayload(payload);
        return event;
    }

    public void markPublished() {
        publishedAt = LocalDateTime.now();
    }

    @PrePersist
    public void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
