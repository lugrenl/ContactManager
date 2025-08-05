package org.example.config;

import org.example.dao.UserDao;
import org.example.entity.User;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.*;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

@Configuration
@Profile("test")
@ComponentScan("org.example")
@Import({org.example.config.HibernateConfig.class})
public class TestConfig {

    @Bean
    public CommandLineRunner initTestData(PasswordEncoder passwordEncoder, UserDao userDao) {
        return args -> {
            // Create test users if they don't exist
            createUserIfNotExists("testuser", "testpass123", "ROLE_USER", "test@example.com", passwordEncoder, userDao);
            createUserIfNotExists("admin", "adminpass", "ROLE_ADMIN", "admin@example.com", passwordEncoder, userDao);
        };
    }

    private void createUserIfNotExists(String username, String password, String role, String email, 
                                     PasswordEncoder passwordEncoder, UserDao userDao) {
        try {
            Optional<User> existingUser = userDao.findByUsername(username);
            if (existingUser.isEmpty()) {
                User user = new User();
                user.setName(username);
                user.setPassword(passwordEncoder.encode(password));
                user.setEmail(email);
                user.setRole(role);
                userDao.addUser(user);
            }
        } catch (Exception e) {
            // Log error if user creation fails
            System.err.println("Error creating test user " + username + ": " + e.getMessage());
        }
    }
}
