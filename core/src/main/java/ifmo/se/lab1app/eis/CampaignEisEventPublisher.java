package ifmo.se.lab1app.eis;

import com.fasterxml.jackson.databind.ObjectMapper;
import ifmo.se.lab1app.auth.domain.UserAccount;
import ifmo.se.lab1app.client.domain.creative.Creative;
import ifmo.se.lab1app.client.domain.enums.CampaignObjective;
import ifmo.se.lab1app.client.domain.enums.CampaignType;
import ifmo.se.lab1app.client.domain.enums.StartMode;
import ifmo.se.lab1app.client.infra.CreativeRepository;
import ifmo.se.lab1app.shared.domain.Campaign;
import ifmo.se.lab1app.shared.domain.CampaignStatus;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CampaignEisEventPublisher {

    private final EisIntegrationProperties properties;
    private final CreativeRepository creativeRepository;
    private final ObjectMapper objectMapper;
    private volatile Object connectionFactory;

    public CampaignEisEventPublisher(
            EisIntegrationProperties properties,
            CreativeRepository creativeRepository,
            ObjectMapper objectMapper
    ) {
        this.properties = properties;
        this.creativeRepository = creativeRepository;
        this.objectMapper = objectMapper;
    }

    public void publish(
            String eventType,
            CampaignStatus statusBefore,
            CampaignStatus statusAfter,
            Campaign campaign
    ) {
        // Отправляем в EIS событие с параметрами: eventType, statusBefore, statusAfter, campaign.
        publish(eventType, statusBefore, statusAfter, null, campaign, null);
    }

    public void publish(
            String eventType,
            CampaignStatus statusBefore,
            CampaignStatus statusAfter,
            Campaign campaign,
            UserAccount operationUser
    ) {
        // Отправляем в EIS событие с параметрами: eventType, statusBefore, statusAfter, campaign, operationUser.
        publish(eventType, statusBefore, statusAfter, null, campaign, operationUser);
    }

    public void publish(
            String eventType,
            CampaignStatus statusBefore,
            CampaignStatus statusAfter,
            String comment,
            Campaign campaign
    ) {
        // Отправляем в EIS событие с параметрами: eventType, statusBefore, statusAfter, comment, campaign.
        publish(eventType, statusBefore, statusAfter, comment, campaign, null);
    }

    public void publish(
            String eventType,
            CampaignStatus statusBefore,
            CampaignStatus statusAfter,
            String comment,
            Campaign campaign,
            UserAccount operationUser
    ) {
        // Отправляем в EIS событие с параметрами: eventType, statusBefore, statusAfter, comment, campaign, operationUser.
        if (!properties.enabled()) {
            return;
        }
        try {
            String payload = buildPayload(
                    UUID.randomUUID().toString(),
                    eventType,
                    statusBefore,
                    statusAfter,
                    comment,
                    campaign,
                    operationUser
            );
            publishStoredPayload(payload);
            log.info("Published campaign event to EIS eventType={} campaignId={}", eventType, campaign.getId());
        } catch (Exception exception) {
            if (properties.failOnError()) {
                throw new IllegalStateException("Failed to publish campaign event to EIS", exception);
            }
            log.warn("Failed to publish campaign event to EIS eventType={} campaignId={}", eventType, campaign.getId(), exception);
        }
    }

    public String buildPayload(
            String eventId,
            String eventType,
            CampaignStatus statusBefore,
            CampaignStatus statusAfter,
            String comment,
            Campaign campaign
    ) {
        return buildPayload(eventId, eventType, statusBefore, statusAfter, comment, campaign, null);
    }

    public String buildPayload(
            String eventId,
            String eventType,
            CampaignStatus statusBefore,
            CampaignStatus statusAfter,
            String comment,
            Campaign campaign,
            UserAccount operationUser
    ) {
        try {
            return objectMapper.writeValueAsString(toEvent(
                    eventId,
                    eventType,
                    statusBefore,
                    statusAfter,
                    comment,
                    campaign,
                    operationUser
            ));
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to serialize campaign event for EIS", exception);
        }
    }

    public void publishStoredPayload(String payload) {
        if (!properties.enabled()) {
            throw new IllegalStateException("EIS integration is disabled");
        }
        try {
            sendWithJca(payload);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to publish campaign event to EIS", exception);
        }
    }

    private CampaignEisEvent toEvent(
            String eventId,
            String eventType,
            CampaignStatus statusBefore,
            CampaignStatus statusAfter,
            String comment,
            Campaign campaign,
            UserAccount operationUser
    ) {
        List<CreativeEisDto> creatives = campaign.getId() == null
                ? List.of()
                : creativeRepository.findAllByCampaignIdOrderByIdDesc(campaign.getId()).stream()
                        .map(CreativeEisDto::from)
                        .toList();
        return new CampaignEisEvent(
                eventId,
                eventType,
                statusBefore,
                statusAfter,
                comment,
                UserEisDto.from(operationUser != null ? operationUser : campaign.getOwner()),
                CampaignEisDto.from(campaign, creatives)
        );
    }

    private void sendWithJca(String payload) throws Exception {
        Object factory = connectionFactory();
        Object connection = null;
        try {
            Method getConnection = factory.getClass().getMethod("getConnection");
            connection = getConnection.invoke(factory);
            Method send = connection.getClass().getMethod("send", String.class);
            send.invoke(connection, payload);
        } finally {
            if (connection != null) {
                Method close = connection.getClass().getMethod("close");
                close.invoke(connection);
            }
        }
    }

    private Object connectionFactory() throws NamingException {
        Object current = connectionFactory;
        if (current != null) {
            return current;
        }
        Object lookedUp = new InitialContext().lookup(properties.connectionFactoryJndiName());
        connectionFactory = lookedUp;
        return lookedUp;
    }

    private record CampaignEisEvent(
            String eventId,
            String eventType,
            CampaignStatus statusBefore,
            CampaignStatus statusAfter,
            String comment,
            UserEisDto user,
            CampaignEisDto campaign
    ) {
    }

    private record CampaignEisDto(
            Long id,
            String name,
            CampaignObjective objective,
            CampaignType type,
            String url,
            StartMode startMode,
            CampaignStatus status,
            BigDecimal budgetAmount,
            String requestedStartAt,
            Integer durationDays,
            String actualStartAt,
            String actualEndAt,
            Collection<CreativeEisDto> creatives,
            UserEisDto user,
            String moderationComment,
            String paymentConfirmationUrl,
            String paymentId,
            String createdAt,
            String updatedAt
    ) {

        private static CampaignEisDto from(Campaign campaign, Collection<CreativeEisDto> creatives) {
            return new CampaignEisDto(
                    campaign.getId(),
                    campaign.getName(),
                    campaign.getObjective(),
                    campaign.getType(),
                    campaign.getUrl(),
                    campaign.getStartMode(),
                    campaign.getStatus(),
                    campaign.getBudgetAmount(),
                    format(campaign.getRequestedStartAt()),
                    campaign.getDurationDays(),
                    format(campaign.getActualStartAt()),
                    format(campaign.getActualEndAt()),
                    creatives,
                    UserEisDto.from(campaign.getOwner()),
                    campaign.getModerationComment(),
                    campaign.getPaymentConfirmationUrl(),
                    campaign.getPaymentId(),
                    format(campaign.getCreatedAt()),
                    format(campaign.getUpdatedAt())
            );
        }

        private static String format(LocalDateTime value) {
            return value == null ? null : value.toString();
        }
    }

    private record CreativeEisDto(
            Long id,
            String name,
            String type
    ) {

        private static CreativeEisDto from(Creative creative) {
            return new CreativeEisDto(creative.getId(), creative.getName(), creative.getType().name());
        }
    }

    private record UserEisDto(
            Long id,
            String username,
            String role
    ) {

        private static UserEisDto from(UserAccount user) {
            if (user == null) {
                return null;
            }
            return new UserEisDto(
                    user.getId(),
                    user.getUsername(),
                    user.getRole() == null ? null : user.getRole().name()
            );
        }
    }
}
