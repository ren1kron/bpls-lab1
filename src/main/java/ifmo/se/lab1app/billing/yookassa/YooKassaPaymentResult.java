package ifmo.se.lab1app.billing.yookassa;

public record YooKassaPaymentResult(
    String id,
    String status,
    String confirmationUrl
) {
}
