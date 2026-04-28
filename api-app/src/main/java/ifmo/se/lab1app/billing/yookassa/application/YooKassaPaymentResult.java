package ifmo.se.lab1app.billing.yookassa.application;

public record YooKassaPaymentResult(
    String id,
    String status,
    String confirmationUrl
) {
}
