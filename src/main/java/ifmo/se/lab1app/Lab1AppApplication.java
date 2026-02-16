package ifmo.se.lab1app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class Lab1AppApplication {

    public static void main(String[] args) {
        SpringApplication.run(Lab1AppApplication.class, args);
    }

}
