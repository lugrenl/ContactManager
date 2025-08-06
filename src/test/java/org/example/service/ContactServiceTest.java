package org.example.service;

import org.example.dao.ContactDao;
import org.example.dto.ResponseContactDto;
import org.example.entity.Contact;
import org.example.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.annotation.DirtiesContext;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ContactServiceTest {

    @Mock
    private ContactDao contactDao;

    @Mock
    private UserService userService;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private ContactService contactService;

    private User testUser;
    private Contact testContact;

    @BeforeEach
    void setUp() {
        // Setup test user
        testUser = new User();
        testUser.setId(1L);
        testUser.setName("testuser");
        testUser.setPassword("password");
        testUser.setEmail("test@example.com");

        // Setup test contact
        testContact = new Contact();
        testContact.setId(1L);
        testContact.setName("John");
        testContact.setSurname("Doe");
        testContact.setEmail("john.doe@example.com");
        testContact.setPhoneNumber("+1234567890");

        // Setup security context
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("testuser");
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    void addContactToCurrentUser_NewContact_Success() {
        // Arrange
        when(userService.getUserByUsername("testuser")).thenReturn(testUser);
        when(contactDao.findExistingContact(any(), any(), any(), any()))
                .thenReturn(Optional.empty());
        when(contactDao.addContact(any(Contact.class))).thenReturn(1L);

        // Act
        ResponseContactDto result = contactService.addContactToCurrentUser(testContact);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        verify(userService).updateUser(eq(testUser.getId()), any(User.class));
        verify(contactDao).addContact(any(Contact.class));
    }

    @Test
    void getContact_UserOwnsContact_ReturnsContact() {
        // Arrange
        testUser.addContact(testContact);
        when(userService.getUserByUsername("testuser")).thenReturn(testUser);
        when(contactDao.getContact(1L)).thenReturn(testContact);

        // Act
        ResponseContactDto result = contactService.getContact(1L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    void getContact_UserDoesNotOwnContact_ThrowsAccessDenied() {
        // Arrange
        when(userService.getUserByUsername("testuser")).thenReturn(testUser);
        when(contactDao.getContact(1L)).thenReturn(testContact);

        // Act & Assert
        assertThatThrownBy(() -> contactService.getContact(1L))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Contact not belongs to user");
    }

    @Test
    void updateContactForCurrentUser_Success() {
        // Arrange
        testUser.addContact(testContact);
        when(userService.getUserByUsername("testuser")).thenReturn(testUser);
        when(contactDao.getContact(1L)).thenReturn(testContact);

        Contact updatedContact = new Contact();
        updatedContact.setName("Updated");
        updatedContact.setSurname("Contact");
        updatedContact.setEmail("updated@example.com");
        updatedContact.setPhoneNumber("+1987654321");

        // Act
        ResponseContactDto result = contactService.updateContactForCurrentUser(1L, updatedContact);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Updated");
        verify(userService).updateUser(eq(testUser.getId()), any(User.class));
    }

    @Test
    void deleteContactFromCurrentUser_Success() {
        // Arrange
        testUser.addContact(testContact);
        when(userService.getUserByUsername("testuser")).thenReturn(testUser);
        when(contactDao.getContact(1L)).thenReturn(testContact);

        // Act
        contactService.deleteContactFromCurrentUser(1L);

        // Assert
        assertThat(testUser.getContacts()).doesNotContain(testContact);
        verify(userService).updateUser(eq(testUser.getId()), any(User.class));
        verify(contactDao).deleteContact(1L);
    }

    @Test
    void getContactsForCurrentUser_ReturnsUserContacts() {
        // Arrange
        testUser.addContact(testContact);
        when(userService.getUserByUsername("testuser")).thenReturn(testUser);

        // Act
        Set<ResponseContactDto> result = contactService.getContactsForCurrentUser();

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.iterator().next().getId()).isEqualTo(1L);
    }

    @Test
    void updateContact_ContactNotOwned_ThrowsAccessDenied() {
        // Arrange
        when(userService.getUserByUsername("testuser")).thenReturn(testUser);
        when(contactDao.getContact(1L)).thenReturn(testContact);

        // Act & Assert
        assertThatThrownBy(() -> contactService.updateContactForCurrentUser(1L, new Contact()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void deleteContact_ContactNotOwned_ThrowsAccessDenied() {
        // Arrange
        when(userService.getUserByUsername("testuser")).thenReturn(testUser);
        when(contactDao.getContact(1L)).thenReturn(testContact);

        // Act & Assert
        assertThatThrownBy(() -> contactService.deleteContactFromCurrentUser(1L))
                .isInstanceOf(AccessDeniedException.class);
    }
}
