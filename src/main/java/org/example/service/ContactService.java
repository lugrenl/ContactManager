package org.example.service;

import org.example.dto.ContactDto;
import org.example.dao.ContactDao;

import org.example.model.Contact;
import org.example.util.ContactReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.file.Paths;
import java.util.List;

@Service
public class ContactService {
    private final ContactDao contactDao;
    private final ContactReader contactReader;

    @Autowired
    public ContactService(ContactDao contactDao, ContactReader contactReader) {
        this.contactDao = contactDao;
        this.contactReader = contactReader;
    }

    public long addContact(Contact contact) {
        return contactDao.addContact(contact);
    }

    public ContactDto getContact(long contactId) {
        return new ContactDto(contactDao.getContact(contactId));
    }

    public List<ContactDto> getAllContacts() {
        return contactDao.getAllContacts().stream().map(ContactDto::new).toList();
    }

    public ContactDto updateContact(long contactId, Contact contact) {
        return new ContactDto(contactDao.updateContact(contactId, contact));
    }

    public void deleteContact(long contactId) {
        contactDao.deleteContact(contactId);
    }

    public void saveAll(String filePath) {
        var contacts = contactReader.readFromFile(Paths.get(filePath));
        contactDao.saveAll(contacts);
    }
}
