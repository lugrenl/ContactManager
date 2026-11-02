package org.example.config;

import org.example.dao.UserDao;
import org.example.entity.User;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Optional;

@Configuration
@Profile("test")
@ComponentScan("org.example")
@Import({org.example.config.HibernateConfig.class})
public class TestConfig {

    @Bean
    public CommandLineRunner initTestData(PasswordEncoder passwordEncoder, UserDao userDao,
                                          PlatformTransactionManager transactionManager) {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        return args -> transactionTemplate.executeWithoutResult(status -> {
            // The administrator is created from admin.* properties by ContactsManagerConfig.
            createUserIfNotExists("testuser", "testpass123", "ROLE_USER", "test@example.com", passwordEncoder, userDao);
        });
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
