package ifmo.se.lab1app.client.api.dto;

import jakarta.validation.constraints.FutureOrPresent;

import java.time.LocalDateTime;

public record PaymentReceivedRequest(@FutureOrPresent LocalDateTime paidAt) {
}
