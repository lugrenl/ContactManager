package org.example.dao;

import org.example.entity.Contact;
import org.example.exceptions.ContactNotFoundException;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HibernateContactDaoTests {

    @Mock
    private SessionFactory sessionFactory;

    @Mock
    private Session session;

    @Mock
    private Query<Contact> query;

    @InjectMocks
    private HibernateContactDao contactDao;

    private Contact testContact;

    @BeforeEach
    void setUp() {
        when(sessionFactory.openSession()).thenReturn(session);
        
        testContact = new Contact();
        testContact.setId(1L);
        testContact.setName("John");
        testContact.setSurname("Doe");
        testContact.setEmail("john.doe@example.com");
        testContact.setPhoneNumber("+1234567890");
    }

    @Test
    void testGetAllContacts() {
        // Arrange
        List<Contact> expectedContacts = Arrays.asList(testContact, new Contact(), new Contact());
        when(session.createQuery(anyString(), eq(Contact.class))).thenReturn(query);
        when(query.getResultList()).thenReturn(expectedContacts);

        // Act
        List<Contact> result = contactDao.getAllContacts();

        // Assert
        assertNotNull(result);
        assertEquals(3, result.size());
        verify(session).createQuery("from Contact", Contact.class);
    }

    @Test
    void testGetContact_ContactExists() {
        // Arrange
        when(session.get(Contact.class, 1L)).thenReturn(testContact);

        // Act
        Contact result = contactDao.getContact(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("John", result.getName());
        assertEquals("Doe", result.getSurname());
    }

    @Test
    void testGetContact_ContactNotExists() {
        // Arrange
        when(session.get(Contact.class, 999L)).thenReturn(null);

        // Act & Assert
        assertThrows(ContactNotFoundException.class, () -> contactDao.getContact(999L));
    }

    @Test
    void testAddContact() {
        // Arrange
        org.hibernate.Transaction transaction = mock(org.hibernate.Transaction.class);
        when(session.beginTransaction()).thenReturn(transaction);
        when(session.save(any(Contact.class))).thenReturn(1L);

        // Act
        long contactId = contactDao.addContact(testContact);

        // Assert
        assertEquals(1L, contactId);
        verify(session).beginTransaction();
        verify(session).save(any(Contact.class));
        verify(transaction).commit();
    }

    @Test
    void testUpdateContact_ContactExists() {
        // Arrange
        Contact updatedContact = new Contact();
        updatedContact.setName("Jane");
        updatedContact.setSurname("Smith");
        updatedContact.setEmail("jane.smith@example.com");
        updatedContact.setPhoneNumber("+1987654321");

        org.hibernate.Transaction transaction = mock(org.hibernate.Transaction.class);
        when(session.beginTransaction()).thenReturn(transaction);
        when(session.get(Contact.class, 1L)).thenReturn(testContact);

        // Act
        Contact result = contactDao.updateContact(1L, updatedContact);

        // Assert
        assertNotNull(result);
        assertEquals("Jane", result.getName());
        assertEquals("Smith", result.getSurname());
        assertEquals("jane.smith@example.com", result.getEmail());
        assertEquals("+1987654321", result.getPhoneNumber());

        verify(session).beginTransaction();
        verify(session).get(Contact.class, 1L);
        verify(transaction).commit();
    }

    @Test
    void testUpdateContact_ContactNotExists() {
        // Arrange
        Contact updatedContact = new Contact();
        when(session.get(Contact.class, 999L)).thenReturn(null);
        when(session.getTransaction()).thenReturn(mock(org.hibernate.Transaction.class));

        // Act & Assert
        assertThrows(ContactNotFoundException.class, 
            () -> contactDao.updateContact(999L, updatedContact));
        verify(session.getTransaction(), never()).commit();
    }

    @Test
    void testDeleteContact_ContactExists() {
        // Arrange
        org.hibernate.Transaction transaction = mock(org.hibernate.Transaction.class);
        when(session.beginTransaction()).thenReturn(transaction);
        when(session.get(Contact.class, 1L)).thenReturn(testContact);

        // Act
        contactDao.deleteContact(1L);

        // Assert
        verify(session).beginTransaction();
        verify(session).get(Contact.class, 1L);
        verify(session).remove(testContact);
        verify(transaction).commit();
    }

    @Test
    void testDeleteContact_ContactNotExists() {
        // Arrange
        when(session.get(Contact.class, 999L)).thenReturn(null);
        when(session.getTransaction()).thenReturn(mock(org.hibernate.Transaction.class));

        // Act & Assert
        assertThrows(ContactNotFoundException.class, 
            () -> contactDao.deleteContact(999L));
        verify(session, never()).remove(any(Contact.class));
        verify(session.getTransaction(), never()).commit();
    }

    @Test
    void testFindExistingContact_ContactExists() {
        // Arrange
        when(session.createQuery(anyString(), eq(Contact.class))).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.uniqueResultOptional()).thenReturn(Optional.of(testContact));

        // Act
        Optional<Contact> result = contactDao.findExistingContact(
            "John", "Doe", "john.doe@example.com", "+1234567890");

        // Assert
        assertTrue(result.isPresent());
        assertEquals("John", result.get().getName());
        assertEquals("Doe", result.get().getSurname());
        verify(query).setParameter("name", "John");
        verify(query).setParameter("surname", "Doe");
        verify(query).setParameter("email", "john.doe@example.com");
        verify(query).setParameter("phoneNumber", "+1234567890");
    }

    @Test
    void testFindExistingContact_ContactNotExists() {
        // Arrange
        when(session.createQuery(anyString(), eq(Contact.class))).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.uniqueResultOptional()).thenReturn(Optional.empty());

        // Act
        Optional<Contact> result = contactDao.findExistingContact(
            "Nonexistent", "Contact", "nonexistent@example.com", "+0000000000");

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    void testSaveAll() {
        // Arrange
        List<Contact> contacts = Arrays.asList(
                new Contact(), new Contact(), new Contact()
        );
        org.hibernate.Transaction transaction = mock(org.hibernate.Transaction.class);
        when(session.beginTransaction()).thenReturn(transaction);

        // Act
        contactDao.saveAll(contacts);

        // Assert
        verify(session).beginTransaction();
        verify(session, times(3)).save(any(Contact.class));
        verify(transaction).commit();
    }
}
