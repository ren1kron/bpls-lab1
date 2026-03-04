package ifmo.se.lab1app.billing.yookassa.application;

import com.fasterxml.jackson.databind.JsonNode;
import ifmo.se.lab1app.billing.yookassa.dto.PaymentObject;
import ifmo.se.lab1app.billing.yookassa.dto.YooKassaNotification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/notification")
public class YooKassaWebhook {

    private static final Logger log = LoggerFactory.getLogger(YooKassaWebhook.class);

    @PostMapping
    public ResponseEntity<Void> handleWebhook(@RequestBody JsonNode requestBody) {
        try {
            YooKassaNotification notification = YooKassaNotification.fromJson(requestBody);

            PaymentObject payment = notification.object();
            log.info("Received event={} paymentId={} status={} paid={}",
                    notification.event(),
                    payment.id(),
                    payment.status(),
                    payment.paid());

            switch (notification.event()) {
                case "payment.succeeded" -> log.info("Handle successful payment {}", payment.id());
                case "payment.canceled" -> log.info("Handle canceled payment {}", payment.id());
                default -> log.warn("Unsupported event: {}", notification.event());
            }

            return ResponseEntity.ok().build();
        } catch (Exception ex) {
            log.error("Invalid webhook payload", ex);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }
}
