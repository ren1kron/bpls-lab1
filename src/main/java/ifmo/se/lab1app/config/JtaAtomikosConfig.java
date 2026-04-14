package ifmo.se.lab1app.config;

import com.atomikos.icatch.jta.UserTransactionImp;
import com.atomikos.icatch.jta.UserTransactionManager;
import com.atomikos.jdbc.AtomikosDataSourceBean;
import jakarta.transaction.SystemException;
import jakarta.transaction.TransactionManager;
import jakarta.transaction.UserTransaction;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.hibernate.SpringJtaPlatform;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.jta.JtaTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Configuration
public class JtaAtomikosConfig {

    @Bean(initMethod = "init", destroyMethod = "close")
    @Primary
    @ConfigurationProperties("app.atomikos.datasource")
    AtomikosDataSourceBean mainDataSource() {
        return new AtomikosDataSourceBean();
    }

    @Bean(initMethod = "init", destroyMethod = "close")
    @ConfigurationProperties("app.atomikos.creative-datasource")
    AtomikosDataSourceBean creativeDataSource() {
        return new AtomikosDataSourceBean();
    }

    @Bean(initMethod = "init", destroyMethod = "close")
    UserTransactionManager atomikosTransactionManager() {
        UserTransactionManager transactionManager = new UserTransactionManager();
        transactionManager.setForceShutdown(false);
        return transactionManager;
    }

    @Bean
    UserTransaction atomikosUserTransaction(
            @Value("${app.atomikos.default-jta-timeout-seconds:60}") int defaultJtaTimeoutSeconds
    ) throws SystemException {
        UserTransactionImp userTransaction = new UserTransactionImp();
        userTransaction.setTransactionTimeout(defaultJtaTimeoutSeconds);
        return userTransaction;
    }

    @Bean
    JtaTransactionManager transactionManager(
            UserTransaction atomikosUserTransaction,
            TransactionManager atomikosTransactionManager
    ) {
        return new JtaTransactionManager(atomikosUserTransaction, atomikosTransactionManager);
    }

    @Bean("writeTransactionTemplate")
    TransactionTemplate writeTransactionTemplate(PlatformTransactionManager transactionManager) {
        return new TransactionTemplate(transactionManager);
    }

    @Bean("readTransactionTemplate")
    TransactionTemplate readTransactionTemplate(PlatformTransactionManager transactionManager) {
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.setReadOnly(true);
        return template;
    }

    @Bean
    HibernatePropertiesCustomizer jtaHibernatePropertiesCustomizer(
            JtaTransactionManager transactionManager
    ) {
        return properties -> {
            properties.put("hibernate.transaction.coordinator_class", "jta");
            properties.put("hibernate.transaction.jta.platform", new SpringJtaPlatform(transactionManager));
            properties.put("jakarta.persistence.transactionType", "JTA");
        };
    }
}
