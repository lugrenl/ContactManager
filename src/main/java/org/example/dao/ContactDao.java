package org.example.dao;

import org.example.entity.Contact;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ContactDao {
    long addContact(Contact contact);
    Contact getContact(long contactId);
    Contact updateContact(long contactId, Contact contact);
    List<Contact> getAllContacts();
    void deleteContact(long contactId);
    void saveAll(Collection<Contact> contacts);
    Optional<Contact> findExistingContact(String name, String surname, String email, String phoneNumber);
}
