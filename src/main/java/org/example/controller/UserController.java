package org.example.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.dto.RequestUserDto;
import org.example.dto.ResponseUserDto;
import org.example.entity.User;
import org.example.service.UserService;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/users")
public class UserController {
    private final UserService userService;
    private final ModelMapper modelMapper;

    @GetMapping ("/{userId}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<ResponseUserDto> getUser(@PathVariable("userId") Long userId) {
        return ResponseEntity.ok().body(userService.getUser(userId));
    }

    @GetMapping
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public List<ResponseUserDto> getAllUsers() {
        return userService.getAllUsers();
    }

    @PutMapping("/{userId}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<ResponseUserDto> updateUser(@PathVariable("userId") long userId,
                                                      @RequestBody @Valid RequestUserDto requestUserDto,
                                                      BindingResult bindingResult) throws BindException {
        if (bindingResult.hasErrors()) {
            if (bindingResult instanceof BindException exception) {
                throw exception;
            } else {
                throw new BindException(bindingResult);
            }
        } else {
            User user = modelMapper.map(requestUserDto, User.class);
            return ResponseEntity.ok().body(userService.updateUser(userId, user));
        }
    }

    @DeleteMapping("/{userId}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<?> deleteUser(@PathVariable("userId") long userId) {
        userService.deleteContact(userId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
