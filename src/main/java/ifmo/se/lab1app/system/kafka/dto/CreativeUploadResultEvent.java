package ifmo.se.lab1app.system.kafka.dto;

import ifmo.se.lab1app.shared.domain.CreativeUploadStatus;

public record CreativeUploadResultEvent(
        String taskId,
        Long campaignId,
        CreativeUploadStatus status,
        Long creativeId,
        String error
) {
}
