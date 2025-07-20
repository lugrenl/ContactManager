package org.example.util;

import lombok.RequiredArgsConstructor;
import org.example.exceptions.UserNotFoundException;
import org.example.entity.User;
import org.example.service.UserDetailsServiceImpl;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

@Component
@RequiredArgsConstructor
public class UserValidator implements Validator {
    private final UserDetailsServiceImpl userDetailsService;

    @Override
    public boolean supports(Class<?> clazz) {
        return User.class.equals(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        User user = (User) target;
        try {
            userDetailsService.loadUserByUsername(user.getName());
        } catch (UserNotFoundException ignored) {
            return; // User isn't found, validation passes
        }
        errors.rejectValue("name", "username.exists");
    }
}
