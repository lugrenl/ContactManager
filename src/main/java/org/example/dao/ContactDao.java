package org.example.dao;

import org.example.model.Contact;

import java.util.Collection;
import java.util.List;

public interface ContactDao {
    Contact addContactReturnContact(String name, String surname, String email, String phoneNumber);
    long addContact(Contact contact);
    Contact getContact(long contactId);
    Contact updateContact(long contactId, String name, String surname, String email, String phoneNumber);
    List<Contact> getAllContacts();
    void updatePhoneNumber(long contactId, String phoneNumber);
    void updateEmail(long contactId, String email);
    void deleteContact(long contactId);
    void saveAll(Collection<Contact> contacts);
}
