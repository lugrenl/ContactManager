package org.example.service;

import org.example.dto.UserDto;
import org.example.model.User;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {
    public List<UserDto> getAllUsers() {
        return List.of(new UserDto());
    }

    public UserDto getUser(Long userId) {
        return new UserDto();
    }

    public UserDto updateUser(long userId, User user) {
        return new UserDto();
    }

    public void deleteContact(long contactId) {
    }
}
