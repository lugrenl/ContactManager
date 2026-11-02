package org.example.service;

import lombok.RequiredArgsConstructor;
import org.example.dao.UserDao;
import org.example.entity.User;
import org.example.exceptions.UserAlreadyExistsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RegistrationService {
    private final UserDao userDao;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void register(User user) {
        if (userDao.findByUsername(user.getName()).isPresent()) {
            throw new UserAlreadyExistsException("User with this username already exists");
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        if (user.getRole() == null)
            user.setRole("ROLE_USER");
        userDao.addUser(user);
    }
}
