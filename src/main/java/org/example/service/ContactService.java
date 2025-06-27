package org.example.service;

import org.example.dto.ContactDto;
import org.example.dao.ContactDao;

import org.example.model.Contact;
import org.example.model.User;
import org.example.util.ContactReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Paths;
import java.util.List;
import java.util.Set;

@Service
public class ContactService {
    private final ContactDao contactDao;
    private final ContactReader contactReader;
    private final UserService userService;

    @Autowired
    public ContactService(ContactDao contactDao, ContactReader contactReader, UserService userService) {
        this.contactDao = contactDao;
        this.contactReader = contactReader;
        this.userService = userService;
    }

    public long addContact(Contact contact) {
        return contactDao.addContact(contact);
    }

    public ContactDto addContactToCurrentUser(Contact contact) {
        User user = getCurrentUser();
        user.getContacts().add(contact);
        contact.getUsers().add(user);
        long contactId = contactDao.addContact(contact);
        contact.setId(contactId);
        return new ContactDto(contact);
    }

    public ContactDto getContact(long contactId) {
        User user = getCurrentUser();
        if (!user.getContacts().contains(contactDao.getContact(contactId))) {
            throw new AccessDeniedException("Contact not belongs to user");
        }
        return new ContactDto(contactDao.getContact(contactId));
    }

    public List<ContactDto> getAllContacts() {
        return contactDao.getAllContacts().stream().map(ContactDto::new).toList();
    }

    public Set<Contact> getContactsForCurrentUser() {
        User user = getCurrentUser();
        return user.getContacts();
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

    public void deleteContactFromCurrentUser(Long contactId) {
        User user = getCurrentUser();
        Contact contact = contactDao.getContact(contactId);

        if (!user.getContacts().contains(contact)) {
            throw new AccessDeniedException("Contact not found or access denied");
        }
        user.getContacts().remove(contact);
        contact.getUsers().remove(user);

        if (contact.getUsers().isEmpty()) {
            contactDao.deleteContact(contact.getId());
        }
    }

    @Transactional
    public ContactDto updateContactForCurrentUser(Long contactId, Contact newData) {
        // 1. Получаем текущего пользователя
        User currentUser = getCurrentUser();

        // 2. Получаем существующий контакт из БД
        Contact existingContact = contactDao.getContact(contactId);

        // 3. Проверяем, что контакт принадлежит пользователю
        if (!currentUser.getContacts().contains(existingContact)) {
            throw new AccessDeniedException("Contact does not belong to the current user");
        }

        // 4. Создаем КОПИЮ контакта для текущего пользователя
        Contact savedContact = new Contact();
        savedContact.setName(newData.getName());
        savedContact.setSurname(newData.getSurname());
        savedContact.setEmail(newData.getEmail());
        savedContact.setPhoneNumber(newData.getPhoneNumber());

        // 5. Сохраняем новый контакт и получаем его ID
        long savedContactId = contactDao.addContact(savedContact);
        savedContact.setId(savedContactId);

        // 6. Обновляем связи
        existingContact.getUsers().remove(currentUser);
        currentUser.getContacts().remove(existingContact);
        currentUser.getContacts().add(savedContact);
        savedContact.getUsers().add(currentUser);

        // 7. Удаляем старый контакт, если он больше никому не принадлежит
        if (existingContact.getUsers().isEmpty()) {
            contactDao.deleteContact(existingContact.getId());
        }

        // Возвращаем DTO новой версии контакта
        return new ContactDto(savedContact);
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        return userService.geUserByUsername(username);
    }
}

