package ifmo.se.lab1app.system.api;

import ifmo.se.lab1app.system.scheduler.CampaignTimersScheduler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@PreAuthorize("hasRole('COMPANY_MODERATOR')")
@RestController
@RequestMapping("/advertisement/scheduler")
@RequiredArgsConstructor
@Tag(
        name = "Scheduler",
        description = "Временная замена scheduler"
)
public class SchedulerMockController {
    private final CampaignTimersScheduler scheduler;

    @PostMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Запустить задачи scheduler")
    public void runScheduler() {
        scheduler.processTimers();
    }
}
