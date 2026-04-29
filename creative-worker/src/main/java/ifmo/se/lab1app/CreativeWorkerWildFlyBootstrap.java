package ifmo.se.lab1app;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

@WebListener
public class CreativeWorkerWildFlyBootstrap implements ServletContextListener {

    private ConfigurableApplicationContext applicationContext;

    @Override
    public void contextInitialized(ServletContextEvent event) {
        applicationContext = new SpringApplicationBuilder(CreativeWorkerApplication.class)
                .web(WebApplicationType.NONE)
                .run();
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
        if (applicationContext != null) {
            applicationContext.close();
        }
    }
}
