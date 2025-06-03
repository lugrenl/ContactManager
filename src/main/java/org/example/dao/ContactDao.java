package org.example.dao;

import org.example.model.Contact;

import java.util.Collection;
import java.util.List;

public interface ContactDao {
    long addContact(Contact contact);
    Contact getContact(long contactId);
    Contact updateContact(long contactId, Contact contact);
    List<Contact> getAllContacts();
    void deleteContact(long contactId);
    void saveAll(Collection<Contact> contacts);
}
