package ifmo.se.lab1app.billing.yookassa.dto;

import java.util.Map;

public record PaymentObject(String id, String status, Boolean paid, Map<String, Object> rawPayload) {}
