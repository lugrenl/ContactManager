package org.example.controller;

import jakarta.validation.Valid;
import org.example.dto.UserDto;
import org.example.model.User;
import org.example.service.UserService;
import org.modelmapper.ModelMapper;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/users")
public class UserController {
    private final UserService userService;
    private final ModelMapper modelMapper;

    public UserController(UserService userService, ModelMapper modelMapper) {
        this.userService = userService;
        this.modelMapper = modelMapper;
    }

    @GetMapping ("/{userId}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public UserDto getUser(@PathVariable Long userId) {
        return userService.getUser(userId);
    }

    @GetMapping
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public List<UserDto> getAllUsers() {
        return userService.getAllUsers();
    }

    @PutMapping("/{userId}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public UserDto updateUser(@PathVariable("userId") long userId, @RequestBody @Valid UserDto userDto) {
        User user = modelMapper.map(userDto, User.class);
        return userService.updateUser(userId, user);
    }

    @DeleteMapping("/{userId}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public void deleteUser(@PathVariable("userId") long contactId) {
        userService.deleteContact(contactId);
    }
}
