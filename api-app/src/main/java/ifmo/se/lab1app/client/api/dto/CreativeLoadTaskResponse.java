package ifmo.se.lab1app.client.api.dto;

import ifmo.se.lab1app.client.domain.creative.CreativeType;
import ifmo.se.lab1app.shared.domain.CampaignStatus;
import ifmo.se.lab1app.shared.domain.CreativeUploadStatus;
import ifmo.se.lab1app.shared.domain.CreativeUploadTask;

public record CreativeLoadTaskResponse(
        String taskId,
        Long campaignId,
        CreativeUploadStatus status,
        CampaignStatus campaignStatus,
        String url,
        CreativeType type,
        Long creativeId,
        String error
) {

    public static CreativeLoadTaskResponse from(CreativeUploadTask task, CampaignStatus campaignStatus) {
        return new CreativeLoadTaskResponse(
                task.getId(),
                task.getCampaignId(),
                task.getStatus(),
                campaignStatus,
                task.getUrl(),
                task.getType(),
                task.getCreativeId(),
                task.getError()
        );
    }
}
