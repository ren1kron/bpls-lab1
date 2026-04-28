package ifmo.se.lab1app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration;

@SpringBootApplication(exclude = DataJpaRepositoriesAutoConfiguration.class)
@ConfigurationPropertiesScan(basePackages = "ifmo.se.lab1app")
public class CreativeWorkerApplication {

    public static void main(String[] args) {
        SpringApplication.run(CreativeWorkerApplication.class, args);
    }
}
