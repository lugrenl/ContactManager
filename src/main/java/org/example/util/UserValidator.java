package org.example.util;

import lombok.RequiredArgsConstructor;
import org.example.exceptions.UserAlreadyExistsException;
import org.example.exceptions.UserNotFoundException;
import org.example.entity.User;
import org.example.service.UserDetailsServiceImpl;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.springframework.lang.NonNull;

@Component
@RequiredArgsConstructor
public class UserValidator implements Validator {
    private final UserDetailsServiceImpl userDetailsService;

    @Override
    public boolean supports(@NonNull Class<?> clazz) {
        return User.class.equals(clazz);
    }

    @Override
    public void validate(@NonNull Object target, @NonNull Errors errors) {
        User user = (User) target;
        try {
            userDetailsService.loadUserByUsername(user.getName());
            // If we get here, user exists
            throw new UserAlreadyExistsException("User with this username already exists");
        } catch (UserNotFoundException ignored) {
            // User isn't found, validation passes
        }
    }
}
