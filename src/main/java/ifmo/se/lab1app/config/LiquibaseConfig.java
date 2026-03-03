package ifmo.se.lab1app.config;

import jakarta.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import liquibase.integration.spring.SpringLiquibase;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.ObjectUtils;

@Configuration
@ConditionalOnBooleanProperty(name = "spring.liquibase.enabled", matchIfMissing = true)
public class LiquibaseConfig {

    private static final String LIQUIBASE_BEAN_NAME = "liquibase";

    @Bean(name = LIQUIBASE_BEAN_NAME)
    SpringLiquibase liquibase(
            DataSource dataSource,
            @Value("${spring.liquibase.change-log:classpath:db/changelog/db.changelog-master.yaml}") String changeLog
    ) {
        SpringLiquibase liquibase = new SpringLiquibase();
        liquibase.setDataSource(dataSource);
        liquibase.setChangeLog(changeLog);
        return liquibase;
    }

    @Bean
    static BeanFactoryPostProcessor entityManagerFactoryDependsOnLiquibase() {
        return beanFactory -> {
            for (String beanName : beanFactory.getBeanNamesForType(EntityManagerFactory.class, true, false)) {
                BeanDefinition beanDefinition = beanFactory.getBeanDefinition(beanName);
                beanDefinition.setDependsOn(appendDependency(beanDefinition.getDependsOn(), LIQUIBASE_BEAN_NAME));
            }
        };
    }

    private static String[] appendDependency(String[] existingDependsOn, String dependency) {
        if (ObjectUtils.containsElement(existingDependsOn, dependency)) {
            return existingDependsOn;
        }
        if (existingDependsOn == null || existingDependsOn.length == 0) {
            return new String[]{dependency};
        }

        String[] updatedDependsOn = new String[existingDependsOn.length + 1];
        System.arraycopy(existingDependsOn, 0, updatedDependsOn, 0, existingDependsOn.length);
        updatedDependsOn[existingDependsOn.length] = dependency;
        return updatedDependsOn;
    }
}
