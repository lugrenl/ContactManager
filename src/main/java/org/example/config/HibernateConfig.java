package org.example.config;

import org.example.entity.Contact;
import org.example.entity.RevokedToken;
import org.example.entity.User;
import org.hibernate.SessionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.orm.hibernate5.HibernateTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.util.Arrays;

@Configuration
@EnableTransactionManagement
public class HibernateConfig {

    static final String TEST_PROFILE = "test";
    private static final String H2_MEMORY_URL_PREFIX = "jdbc:h2:mem:";
    private static final String H2_DRIVER = "org.h2.Driver";
    private static final String H2_DIALECT_MARKER = "H2Dialect";

    @Bean
    public SessionFactory sessionFactory(Environment environment) {
        String url = requireProperty(environment, "spring.datasource.url");
        String username = requireProperty(environment, "spring.datasource.username");
        String password = environment.getProperty("spring.datasource.password", "");
        String driver = requireProperty(environment, "spring.datasource.driver-class-name");
        String dialect = requireProperty(environment, "spring.jpa.database-platform");
        boolean testProfile = Arrays.asList(environment.getActiveProfiles()).contains(TEST_PROFILE);

        if (testProfile) {
            assertIsolatedTestDatabase(url, driver, dialect);
        }

        var configuration = new org.hibernate.cfg.Configuration()
                .addAnnotatedClass(Contact.class)
                .addAnnotatedClass(User.class)
                .addAnnotatedClass(RevokedToken.class)
                .configure();

        configuration.setProperty("hibernate.connection.url", url);
        configuration.setProperty("hibernate.connection.username", username);
        configuration.setProperty("hibernate.connection.password", password);
        configuration.setProperty("hibernate.connection.driver_class", driver);
        configuration.setProperty("hibernate.dialect", dialect);
        configuration.setProperty("hibernate.hbm2ddl.auto", testProfile ? "create-drop" : "validate");
        configuration.setProperty(
                "hibernate.current_session_context_class",
                "org.springframework.orm.hibernate5.SpringSessionContext");
        return configuration.buildSessionFactory();
    }

    @Bean
    public HibernateTransactionManager transactionManager(SessionFactory sessionFactory) {
        var transactionManager = new HibernateTransactionManager();
        transactionManager.setSessionFactory(sessionFactory);
        transactionManager.setAutodetectDataSource(false);
        return transactionManager;
    }

    private static void assertIsolatedTestDatabase(String url, String driver, String dialect) {
        if (!url.startsWith(H2_MEMORY_URL_PREFIX)) {
            throw new IllegalStateException(
                    "Test profile must use an isolated in-memory H2 database, refusing to start with a non-H2 "
                            + "connection. Configure spring.datasource.url with the '"
                            + H2_MEMORY_URL_PREFIX + "' prefix.");
        }
        if (!H2_DRIVER.equals(driver)) {
            throw new IllegalStateException(
                    "Test profile must use the H2 JDBC driver, refusing to start with driver: " + driver);
        }
        if (!dialect.contains(H2_DIALECT_MARKER)) {
            throw new IllegalStateException(
                    "Test profile must use the H2 Hibernate dialect, refusing to start with dialect: " + dialect);
        }
    }

    private static String requireProperty(Environment environment, String key) {
        String value = environment.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Required database property is missing: " + key);
        }
        return value;
    }
}