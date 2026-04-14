package ifmo.se.lab1app.config;

import ifmo.se.lab1app.client.domain.creative.Creative;
import ifmo.se.lab1app.client.infra.CreativeRepository;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;

@Configuration
@EnableJpaRepositories(
        basePackageClasses = CreativeRepository.class,
        entityManagerFactoryRef = "creativeEntityManagerFactory",
        transactionManagerRef = "transactionManager"
)
public class CreativeJpaConfig {

    @Bean
    LocalContainerEntityManagerFactoryBean creativeEntityManagerFactory(
            EntityManagerFactoryBuilder builder,
            @Qualifier("creativeDataSource") DataSource creativeDataSource
    ) {
        return builder
                .dataSource(creativeDataSource)
                .packages(Creative.class)
                .persistenceUnit("creative")
                .jta(true)
                .build();
    }
}
