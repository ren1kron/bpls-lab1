package ifmo.se.lab1app.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI campaignWorkflowOpenApi() {
        return new OpenAPI()
            .components(new Components())
            .info(new Info()
                .title("Campaign Workflow API")
                .description("REST API для процесса запуска рекламной кампании по BPMN")
                .version("v1")
                .license(new License()
                    .name("Apache 2.0")
                    .url("https://www.apache.org/licenses/LICENSE-2.0")));
    }
}
