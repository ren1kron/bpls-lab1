package ifmo.se.lab1app.shared.api;

import ifmo.se.lab1app.client.api.dto.CampaignResponse;
import ifmo.se.lab1app.shared.application.CommonActivitiesService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(
        name = "Campaigns state control",
        description = "Интерфейс просмотра организаций в системе"
)
public class CommonActivitiesController {

    private final CommonActivitiesService service;

    @GetMapping
    @Operation(summary = "Получить список кампаний")
    public List<CampaignResponse> getCampaigns() {
        return service.getCampaigns();
    }

    @GetMapping("/{campaignId}")
    @Operation(summary = "Получить кампанию")
    public CampaignResponse getCampaign(@PathVariable Long campaignId) {
        return service.getCampaign(campaignId);
    }
}
