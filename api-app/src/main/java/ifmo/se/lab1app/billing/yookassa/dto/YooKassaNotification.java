package ifmo.se.lab1app.billing.yookassa.dto;

import java.util.Map;

public record YooKassaNotification(String event, PaymentObject object) {

    public static YooKassaNotification fromJson(Map<String, Object> root) {
        if (root == null || root.isEmpty()) {
            throw new IllegalArgumentException("Request body is empty");
        }

        Object eventValue = root.get("event");
        Object objectValue = root.get("object");

        if (!(eventValue instanceof String event) || !(objectValue instanceof Map<?, ?> rawObjectMap)) {
            throw new IllegalArgumentException("Required fields 'event' and 'object' are missing");
        }

        Map<String, Object> objectNode = rawObjectMap.entrySet().stream()
            .filter(entry -> entry.getKey() instanceof String)
            .collect(java.util.stream.Collectors.toMap(
                entry -> (String) entry.getKey(),
                Map.Entry::getValue
            ));

        String id = readString(objectNode.get("id"));
        String status = readString(objectNode.get("status"));
        Boolean paid = readBoolean(objectNode.get("paid"));

        PaymentObject paymentObject = new PaymentObject(id, status, paid, objectNode);
        return new YooKassaNotification(event, paymentObject);
    }

    private static String readString(Object value) {
        return value instanceof String stringValue ? stringValue : null;
    }

    private static Boolean readBoolean(Object value) {
        return value instanceof Boolean booleanValue ? booleanValue : null;
    }
}
