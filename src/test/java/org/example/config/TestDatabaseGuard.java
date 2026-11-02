package org.example.config;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;

import java.util.Arrays;

/**
 * Fails the test context before any bean (and therefore any connection) is created
 * when the active test profile does not point at an isolated in-memory H2 database.
 */
public class TestDatabaseGuard implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    private static final String H2_MEMORY_URL_PREFIX = "jdbc:h2:mem:";
    private static final String H2_DRIVER = "org.h2.Driver";

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        ConfigurableEnvironment environment = applicationContext.getEnvironment();
        boolean testProfile = Arrays.asList(environment.getActiveProfiles()).contains(HibernateConfig.TEST_PROFILE);
        if (!testProfile) {
            return;
        }

        String url = environment.getProperty("spring.datasource.url");
        if (url == null || !url.startsWith(H2_MEMORY_URL_PREFIX)) {
            throw new IllegalStateException(
                    "Refusing to start test context: an isolated in-memory H2 database is required, "
                            + "but spring.datasource.url does not start with '" + H2_MEMORY_URL_PREFIX + "'.");
        }

        String driver = environment.getProperty("spring.datasource.driver-class-name");
        if (driver != null && !H2_DRIVER.equals(driver)) {
            throw new IllegalStateException(
                    "Refusing to start test context: expected " + H2_DRIVER + ", but got " + driver + ".");
        }
    }
}