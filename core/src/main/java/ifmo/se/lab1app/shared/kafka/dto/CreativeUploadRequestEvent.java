package ifmo.se.lab1app.shared.kafka.dto;

import ifmo.se.lab1app.client.domain.creative.CreativeType;

public record CreativeUploadRequestEvent(
        String taskId,
        Long campaignId,
        String url,
        CreativeType type
) {
}
