package org.example.config;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.FileAppender;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
class LoggingConfigurationIT {

    @Test
    void logbackSpringConfigurationIsLoaded() {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        Logger root = context.getLogger(Logger.ROOT_LOGGER_NAME);

        assertThat(root.getAppender("CONSOLE")).isNotNull();
        assertThat(root.getAppender("ROLLING_FILE")).isNotNull();

        FileAppender<?> rollingFile = (FileAppender<?>) root.getAppender("ROLLING_FILE");
        assertThat(rollingFile.getFile().replace('\\', '/'))
                .isEqualTo("logs/contact-manager.log");
        assertThat(root.getLevel()).isEqualTo(Level.INFO);
    }

    @Test
    void sqlBindParameterLoggingIsNotEnabledByDefault() {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();

        assertThat(context.getLogger("org.hibernate.SQL").getLevel()).isEqualTo(Level.DEBUG);
        assertThat(context.getLogger("org.hibernate.orm.jdbc.bind").getLevel()).isEqualTo(Level.WARN);
    }
}
