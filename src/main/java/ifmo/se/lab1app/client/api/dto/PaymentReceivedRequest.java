package ifmo.se.lab1app.client.api.dto;

import java.time.LocalDateTime;

public record PaymentReceivedRequest(LocalDateTime paidAt) {
}
