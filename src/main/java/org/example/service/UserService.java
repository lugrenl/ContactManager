package org.example.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.example.dao.UserDao;
import org.example.dto.ResponseUserDto;
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
    public List<ResponseUserDto> getAllUsers() {
        return userDao.getAllUsers().stream().map(ResponseUserDto::new).toList();
    }

    @Transactional
    public ResponseUserDto getUser(Long userId) {
        return userDao.findById(userId)
                .map(ResponseUserDto::new)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));
    }

    @Transactional
    public ResponseUserDto updateUser(long userId, User user) {
        if (user.getPassword() != null) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        return new ResponseUserDto(userDao.updateUser(userId, user));
    }

    @Transactional
    public void deleteUser(long userId) {
        if (!userDao.existsById(userId)) {
            throw new UserNotFoundException("User not found with id: " + userId);
        }
        userDao.deleteUser(userId);
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
