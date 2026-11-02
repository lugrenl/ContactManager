package org.example.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.entity.User;
import org.example.service.RegistrationService;
import org.example.service.UserService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import java.util.Optional;

@Slf4j
@Configuration
@ComponentScan("org.example")
@PropertySource("classpath:jdbc.properties")
@RequiredArgsConstructor
public class ContactsManagerConfig {

    private final RegistrationService registrationService;
    private final UserService userService;

    @Value("${admin.username:}")
    private String adminUsername;

    @Value("${admin.password:}")
    private String adminPassword;

    @Value("${admin.email:admin@example.com}")
    private String adminEmail;

    @Bean
    public ModelMapper modelMapper() {
        return new ModelMapper();
    }

    @Bean
    ApplicationRunner initialAdminCreator() {
        return args -> createAdminUser();
    }

    User createAdminUser() {
        String username = normalize(adminUsername);
        String password = adminPassword == null || adminPassword.isBlank() ? null : adminPassword;
        String email = normalize(adminEmail) == null ? "admin@example.com" : adminEmail.trim();

        if (username == null && password == null) {
            log.warn("Initial administrator is not configured (ADMIN_USERNAME/ADMIN_PASSWORD); skipping creation");
            return null;
        }
        if (username == null || password == null) {
            throw new IllegalStateException(
                    "Initial administrator configuration is incomplete: set both ADMIN_USERNAME and "
                            + "ADMIN_PASSWORD, or neither.");
        }

        log.info("Initial administrator configuration resolved for username '{}' with email '{}'",
                username, email);

        Optional<User> existingAdmin = userService.findUserByUsername(username);
        if (existingAdmin.isPresent()) {
            log.info("Initial administrator '{}' already exists; leaving the account and password unchanged. "
                            + "If login with ADMIN_PASSWORD fails, the stored password differs and must be changed "
                            + "through the admin API or database.",
                    username);
            return existingAdmin.get();
        }

        User user = new User();
        user.setName(username);
        user.setPassword(password);
        user.setEmail(email);
        user.setRole("ROLE_ADMIN");
        registrationService.register(user);
        log.info("Initial administrator '{}' created with role ROLE_ADMIN", username);
        return user;
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
