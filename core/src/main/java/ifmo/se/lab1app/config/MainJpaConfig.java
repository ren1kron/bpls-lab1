package ifmo.se.lab1app.config;

import ifmo.se.lab1app.auth.domain.UserAccount;
import ifmo.se.lab1app.auth.infra.UserAccountRepository;
import ifmo.se.lab1app.shared.domain.Campaign;
import ifmo.se.lab1app.shared.infra.CampaignRepository;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;

@Configuration
@EnableJpaRepositories(
        basePackageClasses = {UserAccountRepository.class, CampaignRepository.class},
        entityManagerFactoryRef = "mainEntityManagerFactory",
        transactionManagerRef = "transactionManager"
)
public class MainJpaConfig {

    @Bean(name = {"mainEntityManagerFactory", "entityManagerFactory"})
    @Primary
    LocalContainerEntityManagerFactoryBean mainEntityManagerFactory(
            EntityManagerFactoryBuilder builder,
            @Qualifier("mainDataSource") DataSource mainDataSource
    ) {
        return builder
                .dataSource(mainDataSource)
                .packages(UserAccount.class, Campaign.class)
                .persistenceUnit("main")
                .jta(true)
                .build();
    }
}
