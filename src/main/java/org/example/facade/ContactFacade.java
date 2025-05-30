package org.example.facade;

import org.example.controller.ContactDto;
import org.example.dao.ContactDao;

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

    public ContactDto addContact(String name, String surname, String email, String phoneNumber) {
        return new ContactDto(contactDao.addContactReturnContact(name, surname, email, phoneNumber));
    }

    public ContactDto getContact(long contactId) {
        return new ContactDto(contactDao.getContact(contactId));
    }

    public List<ContactDto> getAllContacts() {
        return contactDao.getAllContacts().stream().map(ContactDto::new).toList();
    }

    public ContactDto updateContact(long contactId, String name, String surname, String email, String phoneNumber) {
        return new ContactDto(contactDao.updateContact(contactId, name, surname, email, phoneNumber));
    }

    public void updateEmail(long contactId, String email) {
        contactDao.updateEmail(contactId, email);
    }

    public void updatePhoneNumber(long contactId, String phoneNumber) {
        contactDao.updatePhoneNumber(contactId, phoneNumber);
    }

    public void deleteContact(long contactId) {
        contactDao.deleteContact(contactId);
    }

    public void saveAll(String filePath) {
        contactService.saveContacts(Paths.get(filePath));
    }
}
