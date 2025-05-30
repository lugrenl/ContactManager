package org.example.dao;

import org.example.model.Contact;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class InMemoryContactDao implements ContactDao {
    private long contactId = 1L;
    private final Map<Long, Contact> contactIdMap;

    public InMemoryContactDao() { this.contactIdMap = new HashMap<>(); }

    @Override
    public Contact addContactReturnContact(String name, String surname, String phoneNumber, String email) {
        Contact contact = new Contact(contactId, name, surname, phoneNumber, email);
        addContact(contact);
        return contact;
    }

    @Override
    public long addContact(Contact contact) {
        contactIdMap.put(contactId++, contact);
        return contact.getId();
    }

    @Override
    public Contact getContact(long contactId) {
        return Optional.ofNullable(contactIdMap.get(contactId)).orElseThrow(() -> new IllegalArgumentException("Contact not found" + contactId));
    }

    @Override
    public Contact updateContact(long contactId, String name, String surname, String phoneNumber, String email) {
        Contact contact = getContact(contactId);
        contact.setName(name);
        contact.setSurname(surname);
        contact.setPhoneNumber(phoneNumber);
        contact.setEmail(email);
        return contact;
    }

    @Override
    public List<Contact> getAllContacts() {
        return contactIdMap.values().stream().toList();
    }

    @Override
    public void updatePhoneNumber(long contactId, String phoneNumber) {
        Contact contact = getContact(contactId);
        contact.setPhoneNumber(phoneNumber);
    }

    @Override
    public void updateEmail(long contactId, String email) {
        Contact contact = getContact(contactId);
        contact.setEmail(email);
    }

    @Override
    public void deleteContact(long contactId) {
        contactIdMap.remove(contactId);
    }

    @Override
    public void saveAll(Collection<Contact> contacts) {
        contacts.forEach(this::addContact);
    }
}
