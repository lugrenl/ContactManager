package org.example.service;

import org.example.dao.UserDao;
import org.example.dto.UserDto;
import org.example.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {
    private final UserDao userDao;

    @Autowired
    public UserService(UserDao userDao) {
        this.userDao = userDao;
    }

    public List<UserDto> getAllUsers() {
        return userDao.getAllUsers().stream().map(UserDto::new).toList();
    }

    public UserDto getUser(Long userId) {
        return new UserDto(userDao.getUser(userId));
    }

    public UserDto updateUser(long userId, User user) {
        return new UserDto(userDao.updateUser(userId, user));
    }

    public void deleteContact(long contactId) {
        userDao.deleteUser(contactId);
    }
}
