package ifmo.se.lab1app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

@SpringBootApplication(exclude = DataJpaRepositoriesAutoConfiguration.class)
@ConfigurationPropertiesScan(basePackages = "ifmo.se.lab1app")
public class ApiAppApplication extends SpringBootServletInitializer {

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(ApiAppApplication.class);
    }

    public static void main(String[] args) {
        SpringApplication.run(ApiAppApplication.class, args);
    }

}
