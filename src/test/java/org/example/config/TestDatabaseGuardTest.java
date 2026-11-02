package org.example.config;

import org.junit.jupiter.api.Test;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.env.MapPropertySource;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TestDatabaseGuardTest {

    private final TestDatabaseGuard guard = new TestDatabaseGuard();

    @Test
    void acceptsIsolatedInMemoryH2ForTestProfile() {
        var context = testContext("jdbc:h2:mem:contactmanager-test;DB_CLOSE_DELAY=-1", "org.h2.Driver");

        assertThatNoException().isThrownBy(() -> guard.initialize(context));
    }

    @Test
    void rejectsPostgresForTestProfile() {
        var context = testContext("jdbc:postgresql://localhost:5432/contactmanager", "org.postgresql.Driver");

        assertThatThrownBy(() -> guard.initialize(context))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("jdbc:h2:mem:");
    }

    @Test
    void rejectsUnknownDatabaseForTestProfile() {
        var context = testContext("jdbc:mysql://localhost:3306/contactmanager", "com.mysql.cj.jdbc.Driver");

        assertThatThrownBy(() -> guard.initialize(context))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("jdbc:h2:mem:");
    }

    @Test
    void rejectsMissingDataSourceForTestProfile() {
        var context = newContext();
        context.getEnvironment().setActiveProfiles(HibernateConfig.TEST_PROFILE);

        assertThatThrownBy(() -> guard.initialize(context))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("jdbc:h2:mem:");
    }

    @Test
    void ignoresNonTestProfile() {
        var context = testContext("jdbc:postgresql://localhost:5432/contactmanager", "org.postgresql.Driver");
        context.getEnvironment().setActiveProfiles("production");

        assertThatNoException().isThrownBy(() -> guard.initialize(context));
    }

    private GenericApplicationContext testContext(String url, String driver) {
        var context = newContext();
        context.getEnvironment().setActiveProfiles(HibernateConfig.TEST_PROFILE);
        Map<String, Object> properties = new HashMap<>();
        properties.put("spring.datasource.url", url);
        properties.put("spring.datasource.driver-class-name", driver);
        context.getEnvironment().getPropertySources()
                .addFirst(new MapPropertySource("test-database-guard", properties));
        return context;
    }

    private GenericApplicationContext newContext() {
        return new GenericApplicationContext();
    }
}