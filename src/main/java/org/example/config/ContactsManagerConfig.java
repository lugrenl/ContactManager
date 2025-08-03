package org.example.config;

import lombok.RequiredArgsConstructor;
import org.example.entity.User;
import org.example.service.RegistrationService;
import org.modelmapper.ModelMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@ComponentScan("org.example")
@PropertySource("classpath:jdbc.properties")
@RequiredArgsConstructor
public class ContactsManagerConfig {

    private final RegistrationService registrationService;

    @Bean
    public ModelMapper modelMapper() {
        return new ModelMapper();
    }

    @Bean
    User createAdminUser() {
        User user = new User();
        user.setName("admin");
        user.setPassword("12345678");
        user.setEmail("admin@example.com");
        user.setRole("ROLE_ADMIN");
        registrationService.register(user);
        return user;
    }
}
