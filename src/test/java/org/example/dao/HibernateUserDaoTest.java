package org.example.dao;

import org.example.entity.Contact;
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
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
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
    private Contact existingContact;

    @BeforeEach
    void setUp() {
        when(sessionFactory.getCurrentSession()).thenReturn(session);

        existingContact = new Contact();
        existingContact.setId(10L);
        existingContact.setName("Existing");
        existingContact.setSurname("Contact");

        testUser = new User();
        testUser.setId(1);
        testUser.setName("testuser");
        testUser.setPassword("password");
        testUser.setEmail("test@example.com");
        testUser.setRole("ROLE_USER");
        testUser.addContact(existingContact);
    }

    @Test
    void testGetAllUsers() {
        List<User> expectedUsers = Arrays.asList(testUser, new User(), new User());
        when(session.createQuery(anyString(), eq(User.class))).thenReturn(query);
        when(query.getResultList()).thenReturn(expectedUsers);

        List<User> result = userDao.getAllUsers();

        assertNotNull(result);
        assertEquals(3, result.size());
        verify(session).createQuery("from User", User.class);
    }

    @Test
    void testGetUser_UserExists() {
        when(session.get(User.class, 1L)).thenReturn(testUser);

        User result = userDao.getUser(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("testuser", result.getName());
    }

    @Test
    void testGetUser_UserNotExists() {
        when(session.get(User.class, 999L)).thenReturn(null);

        assertThrows(UserNotFoundException.class, () -> userDao.getUser(999L));
    }

    @Test
    void testAddUser() {
        when(session.save(any(User.class))).thenReturn(1L);

        userDao.addUser(testUser);

        verify(session).save(any(User.class));
        verify(session).flush();
    }

    @Test
    void testUpdateUser_PreservesContactsAndUpdatesRole() {
        User updatedUser = new User();
        updatedUser.setName("updateduser");
        updatedUser.setPassword("newpassword");
        updatedUser.setEmail("updated@example.com");
        updatedUser.setRole("ROLE_ADMIN");

        when(session.get(User.class, 1L)).thenReturn(testUser);

        User result = userDao.updateUser(1L, updatedUser);

        assertNotNull(result);
        assertEquals("updateduser", result.getName());
        assertEquals("newpassword", result.getPassword());
        assertEquals("updated@example.com", result.getEmail());
        assertEquals("ROLE_ADMIN", result.getRole());
        assertEquals(Set.of(existingContact), result.getContacts());
        verify(session).get(User.class, 1L);
        verify(session).flush();
    }

    @Test
    void testUpdateUser_WithoutPassword_KeepsExistingPassword() {
        User updatedUser = new User();
        updatedUser.setName("updateduser");
        updatedUser.setEmail("updated@example.com");

        when(session.get(User.class, 1L)).thenReturn(testUser);

        User result = userDao.updateUser(1L, updatedUser);

        assertEquals("password", result.getPassword());
        assertEquals("ROLE_USER", result.getRole());
    }

    @Test
    void testDeleteUser_UserExists() {
        when(session.get(User.class, 1L)).thenReturn(testUser);

        userDao.deleteUser(1L);

        verify(session).get(User.class, 1L);
        verify(session).remove(testUser);
    }

    @Test
    void testDeleteUser_UserNotExists() {
        when(session.get(User.class, 999L)).thenReturn(null);

        assertThrows(UserNotFoundException.class, () -> userDao.deleteUser(999L));
        verify(session, never()).remove(any(User.class));
    }

    @Test
    void testFindByUsername_UserExists() {
        when(session.createQuery(anyString(), eq(User.class))).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.uniqueResultOptional()).thenReturn(Optional.of(testUser));

        Optional<User> result = userDao.findByUsername("testuser");

        assertTrue(result.isPresent());
        assertEquals("testuser", result.get().getName());
        verify(query).setParameter("username", "testuser");
    }

    @Test
    void testFindByUsername_UserNotExists() {
        when(session.createQuery(anyString(), eq(User.class))).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.uniqueResultOptional()).thenReturn(Optional.empty());

        Optional<User> result = userDao.findByUsername("nonexistent");

        assertTrue(result.isEmpty());
    }
}