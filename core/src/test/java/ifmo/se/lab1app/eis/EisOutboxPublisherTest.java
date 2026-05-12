package ifmo.se.lab1app.eis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ifmo.se.lab1app.shared.application.TransactionExecutor;
import ifmo.se.lab1app.shared.domain.EisOutboxEvent;
import ifmo.se.lab1app.shared.infra.EisOutboxEventRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class EisOutboxPublisherTest {

    @Mock
    private EisOutboxEventRepository outboxRepository;

    @Mock
    private TransactionExecutor transactions;

    @Mock
    private CampaignEisEventPublisher eventPublisher;

    private EisOutboxPublisher publisher;

    @BeforeEach
    void setUp() {
        doAnswer(invocation -> {
            invocation.<Runnable>getArgument(0).run();
            return null;
        }).when(transactions).write(any(Runnable.class));

        publisher = new EisOutboxPublisher(
                outboxRepository,
                transactions,
                eventPublisher,
                new EisIntegrationProperties(true, "java:/eis/Test", false, 25, 1000)
        );
    }

    @Test
    void shouldMarkOutboxRowPublishedOnlyAfterSuccessfulSend() {
        EisOutboxEvent event = EisOutboxEvent.unpublished("creative-upload:task-1:creative-added", "CreativeAdded", "{}");
        when(outboxRepository.findUnpublishedForUpdate(any(Pageable.class))).thenReturn(List.of(event));

        publisher.publishBatch();

        verify(eventPublisher).publishStoredPayload("{}");
        assertThat(event.getPublishedAt()).isNotNull();
    }

    @Test
    void shouldLeaveOutboxRowUnpublishedWhenSendFails() {
        EisOutboxEvent event = EisOutboxEvent.unpublished("creative-upload:task-1:creative-added", "CreativeAdded", "{}");
        when(outboxRepository.findUnpublishedForUpdate(any(Pageable.class))).thenReturn(List.of(event));
        doThrow(new IllegalStateException("transport down")).when(eventPublisher).publishStoredPayload("{}");

        assertThatThrownBy(() -> publisher.publishBatch())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("transport down");

        assertThat(event.getPublishedAt()).isNull();
    }
}
