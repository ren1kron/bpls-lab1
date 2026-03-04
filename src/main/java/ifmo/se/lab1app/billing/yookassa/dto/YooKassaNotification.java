package ifmo.se.lab1app.billing.yookassa.dto;

import com.fasterxml.jackson.databind.JsonNode;

public record YooKassaNotification(String event, PaymentObject object) {

    public static YooKassaNotification fromJson(JsonNode root) {
        if (root == null || root.isNull()) {
            throw new IllegalArgumentException("Request body is empty");
        }

        JsonNode eventNode = root.get("event");
        JsonNode objectNode = root.get("object");

        if (eventNode == null || eventNode.isNull() || objectNode == null || objectNode.isNull()) {
            throw new IllegalArgumentException("Required fields 'event' and 'object' are missing");
        }

        String event = eventNode.asText();

        String id = objectNode.path("id").asText(null);
        String status = objectNode.path("status").asText(null);
        Boolean paid = objectNode.has("paid") ? objectNode.path("paid").asBoolean() : null;

        PaymentObject paymentObject = new PaymentObject(id, status, paid, objectNode);
        return new YooKassaNotification(event, paymentObject);
    }
}
