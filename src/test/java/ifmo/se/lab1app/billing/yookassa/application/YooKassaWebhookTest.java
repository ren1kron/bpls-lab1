package ifmo.se.lab1app.billing.yookassa.application;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import org.junit.jupiter.api.Test;
import ifmo.se.lab1app.system.application.SystemService;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class YooKassaWebhookTest {

    @Test
    void acceptsPaymentSucceededNotification() throws Exception {
        SystemService systemService = mock(SystemService.class);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new YooKassaWebhook(systemService)).build();

        String payload = """
            {
              "event": "payment.succeeded",
              "object": {
                "id": "2f9e7d1a-000f-5000-9000-1ed0f7f7bc4e",
                "status": "succeeded",
                "paid": true,
                "amount": {
                  "value": "100.00",
                  "currency": "RUB"
                },
                "description": "Test webhook payment"
              }
            }
            """;

        mockMvc.perform(post("/notification")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isOk());

        verify(systemService).processPayment(argThat(notification ->
            "payment.succeeded".equals(notification.event())
                && notification.object() != null
                && "2f9e7d1a-000f-5000-9000-1ed0f7f7bc4e".equals(notification.object().id())
        ));
    }
}
