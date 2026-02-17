package ifmo.se.lab1app.campaign.model;

import ifmo.se.lab1app.campaign.model.creative.Creative;
import ifmo.se.lab1app.campaign.model.enums.CampaignObjective;
import ifmo.se.lab1app.campaign.model.enums.CampaignType;
import ifmo.se.lab1app.campaign.model.enums.StartMode;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "campaigns")
public class Campaign {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private CampaignObjective objective;

    @Column(nullable = false)
    private CampaignType type;

    @Column(nullable = false)
    private String url;

    @Column(nullable = false)
    private StartMode startMode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CampaignStatus status;

    @Column(precision = 15, scale = 2)
    private BigDecimal budgetAmount;

    @Column
    private LocalDateTime startAt;

    @Column
    private LocalDateTime endAt;

    @Column(columnDefinition = "text")
    private String configuration;

    @OneToMany(mappedBy = "campaign", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("id DESC")
    private List<Creative> creatives = new ArrayList<>();

    @Column(columnDefinition = "text")
    private String validationComment;

    @Column(columnDefinition = "text")
    private String moderationComment;

    @Column
    private Integer invoiceDueDays;

    @Column(nullable = false)
    private Boolean budgetFormed;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "campaign", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("revision DESC")
    private List<Invoice> invoices = new ArrayList<>();

    @OneToMany(mappedBy = "campaign", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("createdAt DESC")
    private List<CampaignHistoryEvent> history = new ArrayList<>();

    @Column
    private String notes;

    @PrePersist
    public void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        budgetFormed = false;
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
