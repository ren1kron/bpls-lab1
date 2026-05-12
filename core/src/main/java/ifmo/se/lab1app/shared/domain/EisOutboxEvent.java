package ifmo.se.lab1app.shared.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(
        name = "eis_outbox_events",
        uniqueConstraints = @UniqueConstraint(name = "uk_eis_outbox_events_event_key", columnNames = "event_key")
)
public class EisOutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_key", nullable = false)
    private String eventKey;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(nullable = false, columnDefinition = "text")
    private String payload;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public static EisOutboxEvent unpublished(String eventKey, String eventType, String payload) {
        EisOutboxEvent event = new EisOutboxEvent();
        event.setEventKey(eventKey);
        event.setEventType(eventType);
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
