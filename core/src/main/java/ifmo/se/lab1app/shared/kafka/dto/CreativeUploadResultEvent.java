package ifmo.se.lab1app.shared.kafka.dto;

import ifmo.se.lab1app.shared.domain.CreativeUploadStatus;

public record CreativeUploadResultEvent(
        String taskId,
        Long campaignId,
        CreativeUploadStatus status,
        Long creativeId,
        String error
) {
}
