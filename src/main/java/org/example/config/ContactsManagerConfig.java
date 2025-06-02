package org.example.config;

import org.modelmapper.ModelMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@ComponentScan("org.example")
@PropertySource("classpath:jdbc.properties")
public class ContactsManagerConfig {

    @Bean
    public ModelMapper modelMapper() {
        return new ModelMapper();
    }
}
