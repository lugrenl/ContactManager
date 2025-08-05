package org.example.dao;

import org.example.entity.User;
import org.example.exceptions.UserNotFoundException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HibernateUserDaoTest {

    @Mock
    private SessionFactory sessionFactory;

    @Mock
    private Session session;

    @Mock
    private Query<User> query;

    @InjectMocks
    private UserDao userDao;

    private User testUser;

    @BeforeEach
    void setUp() {
        when(sessionFactory.openSession()).thenReturn(session);
        
        testUser = new User();
        testUser.setId(1);
        testUser.setName("testuser");
        testUser.setPassword("password");
        testUser.setEmail("test@example.com");
    }

    @Test
    void testGetAllUsers() {
        // Arrange
        List<User> expectedUsers = Arrays.asList(testUser, new User(), new User());
        when(session.createQuery(anyString(), eq(User.class))).thenReturn(query);
        when(query.getResultList()).thenReturn(expectedUsers);

        // Act
        List<User> result = userDao.getAllUsers();

        // Assert
        assertNotNull(result);
        assertEquals(3, result.size());
        verify(session).createQuery("from User", User.class);
    }

    @Test
    void testGetUser_UserExists() {
        // Arrange
        when(session.get(User.class, 1L)).thenReturn(testUser);

        // Act
        User result = userDao.getUser(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("testuser", result.getName());
    }

    @Test
    void testGetUser_UserNotExists() {
        // Arrange
        when(session.get(User.class, 999L)).thenReturn(null);

        // Act & Assert
        assertThrows(UserNotFoundException.class, () -> userDao.getUser(999L));
    }

    @Test
    void testAddUser() {
        // Arrange
        doNothing().when(session).save(any(User.class));
        when(session.getTransaction()).thenReturn(mock(org.hibernate.Transaction.class));

        // Act
        userDao.addUser(testUser);

        // Assert
        verify(session).save(any(User.class));
        verify(session.getTransaction()).commit();
    }

    @Test
    void testUpdateUser() {
        // Arrange
        User updatedUser = new User();
        updatedUser.setName("updateduser");
        updatedUser.setPassword("newpassword");
        updatedUser.setEmail("updated@example.com");

        when(session.get(User.class, 1L)).thenReturn(testUser);
        when(session.merge(any(User.class))).thenReturn(testUser);
        when(session.getTransaction()).thenReturn(mock(org.hibernate.Transaction.class));

        // Act
        User result = userDao.updateUser(1L, updatedUser);

        // Assert
        assertNotNull(result);
        assertEquals("updateduser", result.getName());
        assertEquals("newpassword", result.getPassword());
        assertEquals("updated@example.com", result.getEmail());
        verify(session.getTransaction()).commit();
    }

    @Test
    void testDeleteUser() {
        // Arrange
        when(session.get(User.class, 1L)).thenReturn(testUser);
        when(session.getTransaction()).thenReturn(mock(org.hibernate.Transaction.class));

        // Act
        userDao.deleteUser(1L);

        // Assert
        verify(session).remove(testUser);
        verify(session.getTransaction()).commit();
    }

    @Test
    void testDeleteUser_UserNotExists() {
        // Arrange
        when(session.get(User.class, 999L)).thenReturn(null);
        when(session.getTransaction()).thenReturn(mock(org.hibernate.Transaction.class));

        // Act & Assert
        assertThrows(UserNotFoundException.class, () -> userDao.deleteUser(999L));
        verify(session, never()).remove(any(User.class));
        verify(session.getTransaction(), never()).commit();
    }

    @Test
    void testFindByUsername_UserExists() {
        // Arrange
        when(session.createQuery(anyString(), eq(User.class))).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.uniqueResultOptional()).thenReturn(Optional.of(testUser));

        // Act
        Optional<User> result = userDao.findByUsername("testuser");

        // Assert
        assertTrue(result.isPresent());
        assertEquals("testuser", result.get().getName());
        verify(query).setParameter("username", "testuser");
    }

    @Test
    void testFindByUsername_UserNotExists() {
        // Arrange
        when(session.createQuery(anyString(), eq(User.class))).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.uniqueResultOptional()).thenReturn(Optional.empty());

        // Act
        Optional<User> result = userDao.findByUsername("nonexistent");

        // Assert
        assertTrue(result.isEmpty());
    }
}
