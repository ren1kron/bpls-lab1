package ifmo.se.lab1app.config;

import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.hibernate.autoconfigure.HibernateProperties;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.boot.hibernate.autoconfigure.HibernateSettings;
import org.springframework.boot.jpa.EntityManagerFactoryBuilder;
import org.springframework.boot.jpa.autoconfigure.JpaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;

@Configuration
@EnableConfigurationProperties({JpaProperties.class, HibernateProperties.class})
public class JpaPersistenceUnitSupport {

    @Bean
    EntityManagerFactoryBuilder entityManagerFactoryBuilder(
            JpaProperties jpaProperties,
            HibernateProperties hibernateProperties,
            ObjectProvider<HibernatePropertiesCustomizer> hibernatePropertiesCustomizers
    ) {
        JpaVendorAdapter jpaVendorAdapter = jpaVendorAdapter(jpaProperties);
        return new EntityManagerFactoryBuilder(
                jpaVendorAdapter,
                dataSource -> determineHibernateProperties(jpaProperties, hibernateProperties, hibernatePropertiesCustomizers),
                null
        );
    }

    private JpaVendorAdapter jpaVendorAdapter(JpaProperties jpaProperties) {
        HibernateJpaVendorAdapter adapter = new HibernateJpaVendorAdapter();
        if (jpaProperties.getDatabase() != null) {
            adapter.setDatabase(jpaProperties.getDatabase());
        }
        adapter.setDatabasePlatform(jpaProperties.getDatabasePlatform());
        adapter.setGenerateDdl(jpaProperties.isGenerateDdl());
        adapter.setShowSql(jpaProperties.isShowSql());
        return adapter;
    }

    private Map<String, Object> determineHibernateProperties(
            JpaProperties jpaProperties,
            HibernateProperties hibernateProperties,
            ObjectProvider<HibernatePropertiesCustomizer> hibernatePropertiesCustomizers
    ) {
        List<HibernatePropertiesCustomizer> customizers = hibernatePropertiesCustomizers.orderedStream().toList();
        HibernateSettings settings = new HibernateSettings()
                .ddlAuto(hibernateProperties::getDdlAuto)
                .hibernatePropertiesCustomizers(customizers);
        return hibernateProperties.determineHibernateProperties(jpaProperties.getProperties(), settings);
    }
}
