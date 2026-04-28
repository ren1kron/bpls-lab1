package ifmo.se.lab1app.client.domain.creative;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "creatives")
public class Creative {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private CreativeType type;

    @Column(name = "campaign_id", nullable = false)
    private Long campaignId;

    @Column(name = "upload_task_id", unique = true, length = 36)
    private String uploadTaskId;
}
