package org.example.service;

import lombok.RequiredArgsConstructor;
import org.example.dao.ContactDao;

import org.example.dto.ResponseContactDto;
import org.example.entity.Contact;
import org.example.entity.User;
import org.example.util.ContactReader;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ContactService {
    private final ContactDao contactDao;
    private final ContactReader contactReader;
    private final UserService userService;

    @Transactional
    public ResponseContactDto addContactToCurrentUser(Contact contact) {
        // 1. Получаем текущего пользователя
        User user = getCurrentUser();

        // 2. Проверяем, существует ли уже такой контакт
        Optional<Contact> existingContact = contactDao.findExistingContact(
                contact.getName(),
                contact.getSurname(),
                contact.getEmail(),
                contact.getPhoneNumber()
        );

        // 3. Если контакт существует, используем существующий
        if (existingContact.isPresent()) {
            contact = existingContact.get();
        } else {
            // 4. Если контакта нет, создаем новый
            long contactId = contactDao.addContact(contact);
            contact.setId(contactId);
        }

        // 5. Добавляем связь пользователя с контактом
        user.addContact(contact);

        // 6. Сохраняем изменения
        userService.updateUser(user.getId(), user);

        return new ResponseContactDto(contact);
    }

    @Transactional(readOnly = true)
    public ResponseContactDto getContact(long contactId) {
        User user = getCurrentUser();
        if (!user.getContacts().contains(contactDao.getContact(contactId))) {
            throw new AccessDeniedException("Contact not belongs to user");
        }
        return new ResponseContactDto(contactDao.getContact(contactId));
    }

    @Transactional
    public List<ResponseContactDto> getAllContacts() {
        return contactDao.getAllContacts().stream().map(ResponseContactDto::new).toList();
    }

    @Transactional
    public Set<ResponseContactDto> getContactsForCurrentUser() {
        User user = getCurrentUser();
        return user.getContacts().stream().map(ResponseContactDto::new).collect(Collectors.toSet());
    }

    @Transactional
    public void saveAll(String filePath) {
        var contacts = contactReader.readFromFile(Paths.get(filePath));
        contactDao.saveAll(contacts);
    }

    @Transactional
    public void deleteContactFromCurrentUser(Long contactId) {
        // 1. Получаем текущего пользователя и контакт
        User user = getCurrentUser();
        Contact contact = contactDao.getContact(contactId);

        // 2. Проверяем, что контакт принадлежит пользователю
        if (!user.getContacts().contains(contact)) {
            throw new AccessDeniedException("Contact not found or access denied");
        }

        // 3. Удаляем связь
        user.getContacts().remove(contact);
        contact.getUsers().remove(user);

        // 4. Обновляем пользователя
        userService.updateUser(user.getId(), user);

        // 5. Если контакт не имеет пользователей, удаляем его
        if (contact.getUsers().isEmpty()) {
            contactDao.deleteContact(contact.getId());
        }
    }

    @Transactional
    public ResponseContactDto updateContactForCurrentUser(Long contactId, Contact newData) {
        // 1. Получаем текущего пользователя
        User currentUser = getCurrentUser();

        // 2. Получаем существующий контакт
        Contact existingContact = contactDao.getContact(contactId);

        // 3. Проверяем, что контакт принадлежит пользователю
        if (!currentUser.getContacts().contains(existingContact)) {
            throw new AccessDeniedException("Contact does not belong to the current user");
        }

        // 4. Создаем новый контакт
        Contact savedContact = new Contact();
        savedContact.setName(newData.getName());
        savedContact.setSurname(newData.getSurname());
        savedContact.setEmail(newData.getEmail());
        savedContact.setPhoneNumber(newData.getPhoneNumber());

        // 5. Сохраняем новый контакт
        long savedContactId = contactDao.addContact(savedContact);
        savedContact.setId(savedContactId);

        // 6. Обновляем связь между контактом и пользователем
        existingContact.getUsers().remove(currentUser);
        currentUser.getContacts().remove(existingContact);
        currentUser.addContact(savedContact);

        // 7. Сохраняем пользователя с новой связью
        userService.updateUser(currentUser.getId(), currentUser);

        // 8. Если старый контакт не имеет пользователей, удаляем его
        if (existingContact.getUsers().isEmpty()) {
            contactDao.deleteContact(existingContact.getId());
        }

        // 9. Return the DTO of the new contact
        return new ResponseContactDto(savedContact);
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        return userService.getUserByUsername(username);
    }
}

