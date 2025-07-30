package org.example.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.example.dao.UserDao;
import org.example.dto.UserDto;
import org.example.exceptions.UserNotFoundException;
import org.example.entity.User;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserDao userDao;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public List<UserDto> getAllUsers() {
        return userDao.getAllUsers().stream().map(UserDto::new).toList();
    }

    @Transactional
    public UserDto getUser(Long userId) {
        return new UserDto(userDao.getUser(userId));
    }

    @Transactional
    public UserDto addUser(User user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return new UserDto(userDao.addUser(user));
    }

    @Transactional
    public UserDto updateUser(long userId, User user) {
        if (user.getPassword() != null) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        return new UserDto(userDao.updateUser(userId, user));
    }

    @Transactional
    public void deleteContact(long contactId) {
        userDao.deleteUser(contactId);
    }

    @Transactional
    public User getUserByUsername(String username) throws UsernameNotFoundException {
        Optional<User> user = userDao.findByUsername(username);
        if (user.isEmpty()) {
            throw new UserNotFoundException("User not found with username: " + username);
        }
        return user.get();
    }
}
