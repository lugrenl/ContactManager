package org.example.service;

import org.example.dao.ContactDao;
import org.example.dto.ResponseContactDto;
import org.example.entity.Contact;
import org.example.entity.User;
import org.example.exceptions.ContactAlreadyExistsException;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
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
        testUser = new User();
        testUser.setId(1L);
        testUser.setName("testuser");
        testUser.setPassword("password");
        testUser.setEmail("test@example.com");
        testUser.setRole("ROLE_USER");

        testContact = new Contact();
        testContact.setId(1L);
        testContact.setName("John");
        testContact.setSurname("Doe");
        testContact.setEmail("john.doe@example.com");
        testContact.setPhoneNumber("+1234567890");

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("testuser");
        SecurityContextHolder.setContext(securityContext);
        when(userService.getUserByUsername("testuser")).thenReturn(testUser);
    }

    @Test
    void addContactToCurrentUser_NewContact_SetsOwnerAndPersists() {
        when(contactDao.findExistingContact(eq(1L), any(), any(), any(), any()))
                .thenReturn(Optional.empty());
        when(contactDao.addContact(any(Contact.class))).thenReturn(1L);

        ResponseContactDto result = contactService.addContactToCurrentUser(testContact);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(testContact.getOwner()).isSameAs(testUser);
        assertThat(testUser.getContacts()).contains(testContact);
        verify(contactDao).addContact(any(Contact.class));
    }

    @Test
    void addContactToCurrentUser_ExistingContact_ReusesItWithoutInsert() {
        Contact existing = new Contact(7L, "John", "Doe", "john.doe@example.com", "+1234567890");
        existing.setOwner(testUser);
        when(contactDao.findExistingContact(eq(1L), any(), any(), any(), any()))
                .thenReturn(Optional.of(existing));

        ResponseContactDto result = contactService.addContactToCurrentUser(
                new Contact("John", "Doe", "john.doe@example.com", "+1234567890"));

        assertThat(result.getId()).isEqualTo(7L);
        verify(contactDao, never()).addContact(any(Contact.class));
    }

    @Test
    void getContact_UserOwnsContact_ReturnsContact() {
        testUser.addContact(testContact);
        when(contactDao.getContact(1L)).thenReturn(testContact);

        ResponseContactDto result = contactService.getContact(1L);

        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    void getContact_UserDoesNotOwnContact_ThrowsAccessDenied() {
        User other = new User();
        other.setId(2L);
        testContact.setOwner(other);
        when(contactDao.getContact(1L)).thenReturn(testContact);

        assertThatThrownBy(() -> contactService.getContact(1L))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Contact does not belong to the current user");
    }

    @Test
    void updateContactForCurrentUser_UpdatesInPlaceAndKeepsId() {
        testUser.addContact(testContact);
        Contact updated = new Contact();
        updated.setName("Updated");
        updated.setSurname("Contact");
        updated.setEmail("updated@example.com");
        updated.setPhoneNumber("+1987654321");

        Contact managed = new Contact(1L, "Updated", "Contact", "updated@example.com", "+1987654321");
        managed.setOwner(testUser);

        when(contactDao.getContact(1L)).thenReturn(testContact);
        when(contactDao.findExistingContact(eq(1L), any(), any(), any(), any()))
                .thenReturn(Optional.empty());
        when(contactDao.updateContact(1L, updated)).thenReturn(managed);

        ResponseContactDto result = contactService.updateContactForCurrentUser(1L, updated);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Updated");
        verify(contactDao).updateContact(1L, updated);
        verify(contactDao, never()).addContact(any(Contact.class));
    }

    @Test
    void updateContactForCurrentUser_DuplicateOfAnotherContact_ThrowsConflict() {
        testUser.addContact(testContact);
        Contact other = new Contact(2L, "Jane", "Doe", "jane@example.com", null);
        other.setOwner(testUser);
        Contact changes = new Contact("Jane", "Doe", "jane@example.com", null);

        when(contactDao.getContact(1L)).thenReturn(testContact);
        when(contactDao.findExistingContact(eq(1L), any(), any(), any(), any()))
                .thenReturn(Optional.of(other));

        assertThatThrownBy(() -> contactService.updateContactForCurrentUser(1L, changes))
                .isInstanceOf(ContactAlreadyExistsException.class);
        verify(contactDao, never()).updateContact(anyLong(), any(Contact.class));
    }

    @Test
    void deleteContactFromCurrentUser_RemovesPersonalContact() {
        testUser.addContact(testContact);
        when(contactDao.getContact(1L)).thenReturn(testContact);

        contactService.deleteContactFromCurrentUser(1L);

        assertThat(testUser.getContacts()).doesNotContain(testContact);
    }

    @Test
    void getContactsForCurrentUser_ReturnsOnlyOwnContacts() {
        testUser.addContact(testContact);
        Contact other = new Contact(2L, "Jane", "Doe", "jane@example.com", null);
        User otherUser = new User();
        otherUser.setId(2L);
        other.setOwner(otherUser);

        Set<ResponseContactDto> result = contactService.getContactsForCurrentUser();

        assertThat(result).hasSize(1);
        assertThat(result.iterator().next().getId()).isEqualTo(1L);
    }

    @Test
    void updateContact_ContactNotOwned_ThrowsAccessDenied() {
        User other = new User();
        other.setId(2L);
        testContact.setOwner(other);
        when(contactDao.getContact(1L)).thenReturn(testContact);

        assertThatThrownBy(() -> contactService.updateContactForCurrentUser(1L, new Contact()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void deleteContact_ContactNotOwned_ThrowsAccessDenied() {
        User other = new User();
        other.setId(2L);
        testContact.setOwner(other);
        when(contactDao.getContact(1L)).thenReturn(testContact);

        assertThatThrownBy(() -> contactService.deleteContactFromCurrentUser(1L))
                .isInstanceOf(AccessDeniedException.class);
    }
}
