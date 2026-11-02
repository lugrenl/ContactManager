package org.example.dao;

import org.example.entity.Contact;
import org.example.entity.User;
import org.example.exceptions.ContactNotFoundException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HibernateContactDaoTest {

    @Mock
    private SessionFactory sessionFactory;

    @Mock
    private Session session;

    @Mock
    private Query<Contact> query;

    @InjectMocks
    private HibernateContactDao contactDao;

    private Contact testContact;
    private User owner;

    @BeforeEach
    void setUp() {
        when(sessionFactory.getCurrentSession()).thenReturn(session);

        owner = new User();
        owner.setId(1L);

        testContact = new Contact();
        testContact.setId(1L);
        testContact.setName("John");
        testContact.setSurname("Doe");
        testContact.setEmail("john.doe@example.com");
        testContact.setPhoneNumber("+1234567890");
        testContact.setOwner(owner);
    }

    @Test
    void testGetAllContacts() {
        List<Contact> expectedContacts = Arrays.asList(testContact, new Contact(), new Contact());
        when(session.createQuery(anyString(), eq(Contact.class))).thenReturn(query);
        when(query.getResultList()).thenReturn(expectedContacts);

        List<Contact> result = contactDao.getAllContacts();

        assertNotNull(result);
        assertEquals(3, result.size());
        verify(session).createQuery("select c from Contact c join fetch c.owner", Contact.class);
    }

    @Test
    void testGetContact_ContactExists() {
        when(session.get(Contact.class, 1L)).thenReturn(testContact);

        Contact result = contactDao.getContact(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("John", result.getName());
        assertEquals("Doe", result.getSurname());
    }

    @Test
    void testGetContact_ContactNotExists() {
        when(session.get(Contact.class, 999L)).thenReturn(null);

        assertThrows(ContactNotFoundException.class, () -> contactDao.getContact(999L));
    }

    @Test
    void testAddContact() {
        when(session.save(any(Contact.class))).thenReturn(1L);

        long contactId = contactDao.addContact(testContact);

        assertEquals(1L, contactId);
        verify(session).save(any(Contact.class));
        verify(session, never()).close();
    }

    @Test
    void testUpdateContact_ContactExists() {
        Contact updatedContact = new Contact();
        updatedContact.setName("Jane");
        updatedContact.setSurname("Smith");
        updatedContact.setEmail("jane.smith@example.com");
        updatedContact.setPhoneNumber("+1987654321");

        when(session.get(Contact.class, 1L)).thenReturn(testContact);

        Contact result = contactDao.updateContact(1L, updatedContact);

        assertNotNull(result);
        assertEquals("Jane", result.getName());
        assertEquals("Smith", result.getSurname());
        assertEquals("jane.smith@example.com", result.getEmail());
        assertEquals("+1987654321", result.getPhoneNumber());

        verify(session).get(Contact.class, 1L);
    }

    @Test
    void testUpdateContact_ContactNotExists() {
        Contact updatedContact = new Contact();
        when(session.get(Contact.class, 999L)).thenReturn(null);

        assertThrows(ContactNotFoundException.class,
                () -> contactDao.updateContact(999L, updatedContact));
    }

    @Test
    void testDeleteContact_ContactExists() {
        when(session.get(Contact.class, 1L)).thenReturn(testContact);

        contactDao.deleteContact(1L);

        verify(session).get(Contact.class, 1L);
        verify(session).remove(testContact);
    }

    @Test
    void testDeleteContact_ContactNotExists() {
        when(session.get(Contact.class, 999L)).thenReturn(null);

        assertThrows(ContactNotFoundException.class,
                () -> contactDao.deleteContact(999L));
        verify(session, never()).remove(any(Contact.class));
    }

    @Test
    void testFindExistingContact_ContactExists() {
        when(session.createQuery(anyString(), eq(Contact.class))).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.uniqueResultOptional()).thenReturn(Optional.of(testContact));

        Optional<Contact> result = contactDao.findExistingContact(
                1L, "John", "Doe", "john.doe@example.com", "+1234567890");

        assertTrue(result.isPresent());
        assertEquals("John", result.get().getName());
        assertEquals("Doe", result.get().getSurname());
        verify(query).setParameter("ownerId", 1L);
        verify(query).setParameter("name", "John");
        verify(query).setParameter("surname", "Doe");
        verify(query).setParameter("email", "john.doe@example.com");
        verify(query).setParameter("phoneNumber", "+1234567890");
    }

    @Test
    void testFindExistingContact_ContactNotExists() {
        when(session.createQuery(anyString(), eq(Contact.class))).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.uniqueResultOptional()).thenReturn(Optional.empty());

        Optional<Contact> result = contactDao.findExistingContact(
                1L, "Nonexistent", "Contact", "nonexistent@example.com", "+0000000000");

        assertTrue(result.isEmpty());
    }

    @Test
    void testFindExistingContact_WithNullEmail_UsesIsNullPredicate() {
        when(session.createQuery(anyString(), eq(Contact.class))).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.uniqueResultOptional()).thenReturn(Optional.of(testContact));

        contactDao.findExistingContact(1L, "John", "Doe", null, "+1234567890");

        ArgumentCaptor<String> hqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(session).createQuery(hqlCaptor.capture(), eq(Contact.class));
        assertTrue(hqlCaptor.getValue().contains("c.owner.id = :ownerId"));
        assertTrue(hqlCaptor.getValue().contains("c.email IS NULL"));
        verify(query, never()).setParameter(eq("email"), any());
        verify(query).setParameter("phoneNumber", "+1234567890");
    }

    @Test
    void testFindExistingContact_WithNullPhone_UsesIsNullPredicate() {
        when(session.createQuery(anyString(), eq(Contact.class))).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.uniqueResultOptional()).thenReturn(Optional.of(testContact));

        contactDao.findExistingContact(1L, "John", "Doe", "john.doe@example.com", null);

        ArgumentCaptor<String> hqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(session).createQuery(hqlCaptor.capture(), eq(Contact.class));
        assertTrue(hqlCaptor.getValue().contains("c.owner.id = :ownerId"));
        assertTrue(hqlCaptor.getValue().contains("c.phoneNumber IS NULL"));
        verify(query).setParameter("email", "john.doe@example.com");
        verify(query, never()).setParameter(eq("phoneNumber"), any());
    }

    @Test
    void testFindExistingContact_WithBothOptionalFieldsNull_UsesIsNullPredicates() {
        when(session.createQuery(anyString(), eq(Contact.class))).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.uniqueResultOptional()).thenReturn(Optional.of(testContact));

        contactDao.findExistingContact(1L, "John", "Doe", null, null);

        ArgumentCaptor<String> hqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(session).createQuery(hqlCaptor.capture(), eq(Contact.class));
        assertTrue(hqlCaptor.getValue().contains("c.owner.id = :ownerId"));
        assertTrue(hqlCaptor.getValue().contains("c.email IS NULL"));
        assertTrue(hqlCaptor.getValue().contains("c.phoneNumber IS NULL"));
        verify(query, never()).setParameter(eq("email"), any());
        verify(query, never()).setParameter(eq("phoneNumber"), any());
    }
}