package org.example.service;

import org.example.dao.UserDao;
import org.example.dto.ResponseUserDto;
import org.example.entity.User;
import org.example.exceptions.UserNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserDao userDao;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User testUser;

    @BeforeEach
    void setUp() {
        // Setup test user
        testUser = new User();
        testUser.setId(1);
        testUser.setName("testuser");
        testUser.setPassword("password");
        testUser.setEmail("test@example.com");
        testUser.setRole("ROLE_USER");
    }

    @Test
    void getAllUsers_ReturnsListOfUsers() {
        // Arrange
        when(userDao.getAllUsers()).thenReturn(List.of(testUser));

        // Act
        List<ResponseUserDto> result = userService.getAllUsers();

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(1L);
        assertThat(result.get(0).getName()).isEqualTo("testuser");
        verify(userDao).getAllUsers();
    }

    @Test
    void getUser_WithValidId_ReturnsUser() {
        // Arrange
        when(userDao.getUser(1L)).thenReturn(testUser);

        // Act
        ResponseUserDto result = userService.getUser(1L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("testuser");
        verify(userDao).getUser(1L);
    }

    @Test
    void getUser_WithInvalidId_ThrowsUserNotFoundException() {
        // Arrange
        when(userDao.getUser(999L)).thenThrow(new UserNotFoundException("User not found with ID: 999"));

        // Act & Assert
        assertThatThrownBy(() -> userService.getUser(999L))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("User not found with ID: 999");
        verify(userDao).getUser(999L);
    }

    @Test
    void updateUser_WithPassword_EncodesPassword() {
        // Arrange
        User updatedUser = new User();
        updatedUser.setName("updateduser");
        updatedUser.setPassword("newpassword");
        updatedUser.setEmail("updated@example.com");

        when(passwordEncoder.encode("newpassword")).thenReturn("encodedpassword");
        when(userDao.updateUser(1L, updatedUser)).thenAnswer(invocation -> {
            User user = invocation.getArgument(1);
            user.setId(1);
            return user;
        });

        // Act
        ResponseUserDto result = userService.updateUser(1L, updatedUser);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("updateduser");
        assertThat(updatedUser.getPassword()).isEqualTo("encodedpassword");
        verify(passwordEncoder).encode("newpassword");
        verify(userDao).updateUser(1L, updatedUser);
    }

    @Test
    void updateUser_WithoutPassword_DoesNotEncodePassword() {
        // Arrange
        User updatedUser = new User();
        updatedUser.setName("updateduser");
        updatedUser.setEmail("updated@example.com");
        // password is null

        when(userDao.updateUser(1L, updatedUser)).thenAnswer(invocation -> {
            User user = invocation.getArgument(1);
            user.setId(1);
            return user;
        });

        // Act
        ResponseUserDto result = userService.updateUser(1L, updatedUser);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("updateduser");
        verify(passwordEncoder, never()).encode(any());
        verify(userDao).updateUser(1L, updatedUser);
    }

    @Test
    void deleteContact_WithValidId_DeletesUser() {
        // Act
        userService.deleteContact(1L);

        // Assert
        verify(userDao).deleteUser(1L);
    }

    @Test
    void getUserByUsername_WithValidUsername_ReturnsUser() {
        // Arrange
        when(userDao.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // Act
        User result = userService.getUserByUsername("testuser");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("testuser");
        verify(userDao).findByUsername("testuser");
    }

    @Test
    void getUserByUsername_WithInvalidUsername_ThrowsUserNotFoundException() {
        // Arrange
        when(userDao.findByUsername("nonexistent")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> userService.getUserByUsername("nonexistent"))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("User not found with username: nonexistent");
        verify(userDao).findByUsername("nonexistent");
    }

    @Test
    void updateUser_WhenDaoThrowsException_PropagatesException() {
        // Arrange
        when(userDao.updateUser(anyLong(), any(User.class)))
                .thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        assertThatThrownBy(() -> userService.updateUser(1L, new User()))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Database error");
    }

    @Test
    void deleteContact_WhenDaoThrowsException_PropagatesException() {
        // Arrange
        doThrow(new RuntimeException("Delete failed")).when(userDao).deleteUser(anyLong());

        // Act & Assert
        assertThatThrownBy(() -> userService.deleteContact(1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Delete failed");
    }
}
