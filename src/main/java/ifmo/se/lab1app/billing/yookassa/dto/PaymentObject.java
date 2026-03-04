package ifmo.se.lab1app.billing.yookassa.dto;

import com.fasterxml.jackson.databind.JsonNode;

public record PaymentObject(String id, String status, Boolean paid, JsonNode rawPayload) {}
