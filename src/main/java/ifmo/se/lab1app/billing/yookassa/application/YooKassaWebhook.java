package ifmo.se.lab1app.billing.yookassa.application;

import ifmo.se.lab1app.billing.yookassa.dto.YooKassaNotification;
import ifmo.se.lab1app.system.application.SystemService;
import java.util.Map;
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

    private final SystemService service;

    public YooKassaWebhook(SystemService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<Void> handleWebhook(@RequestBody Map<String, Object> requestBody) {
        try {
            YooKassaNotification notification = YooKassaNotification.fromJson(requestBody);
            var payment = notification.object();
            log.info(
                "Received YooKassa webhook event={} paymentId={} status={} paid={}",
                notification.event(),
                payment.id(),
                payment.status(),
                payment.paid()
            );
            service.processPayment(notification);
            return ResponseEntity.ok().build();
        } catch (Exception ex) {
            log.error("Invalid webhook payload", ex);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }
}
