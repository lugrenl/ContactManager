package org.example.facade;

import org.example.dto.ContactDto;
import org.example.dao.ContactDao;

import org.example.model.Contact;
import org.example.service.ContactService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.file.Paths;
import java.util.List;

@Service
public class ContactFacade {
    private final ContactDao contactDao;
    private final ContactService contactService;

    @Autowired
    public ContactFacade(ContactDao contactDao, ContactService contactService) {
        this.contactDao = contactDao;
        this.contactService = contactService;
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
        contactService.saveContacts(Paths.get(filePath));
    }
}
