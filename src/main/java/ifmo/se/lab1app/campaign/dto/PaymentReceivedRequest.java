package ifmo.se.lab1app.campaign.dto;

import java.time.LocalDateTime;

public record PaymentReceivedRequest(LocalDateTime paidAt) {
}
