package ifmo.se.lab1app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(exclude = DataJpaRepositoriesAutoConfiguration.class)
@EnableScheduling
@ConfigurationPropertiesScan
public class Lab1AppApplication {

    public static void main(String[] args) {
        SpringApplication.run(Lab1AppApplication.class, args);
    }

}
