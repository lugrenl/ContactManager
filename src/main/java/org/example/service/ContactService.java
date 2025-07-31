package org.example.service;

import lombok.RequiredArgsConstructor;
import org.example.dto.ContactDto;
import org.example.dao.ContactDao;

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
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ContactService {
    private final ContactDao contactDao;
    private final ContactReader contactReader;
    private final UserService userService;

    @Transactional
    public ContactDto addContactToCurrentUser(Contact contact) {
        // Get the current user with an open session
        User user = getCurrentUser();
        
        // Add the contact to the user (this updates both sides of the relationship)
        user.addContact(contact);

        // Save the contact (the cascade should handle the relationship)
        long contactId = contactDao.addContact(contact);
        contact.setId(contactId);

        // Explicitly save the user to ensure the relationship is persisted
        // This is needed because User is the owner of the relationship
        userService.updateUser(user.getId(), user);

        return new ContactDto(contact);
    }

    @Transactional
    public ContactDto getContact(long contactId) {
        User user = getCurrentUser();
        if (!user.getContacts().contains(contactDao.getContact(contactId))) {
            throw new AccessDeniedException("Contact not belongs to user");
        }
        return new ContactDto(contactDao.getContact(contactId));
    }

    @Transactional
    public List<ContactDto> getAllContacts() {
        return contactDao.getAllContacts().stream().map(ContactDto::new).toList();
    }

    @Transactional
    public Set<ContactDto> getContactsForCurrentUser() {
        User user = getCurrentUser();
        return user.getContacts().stream().map(ContactDto::new).collect(Collectors.toSet());
    }

    @Transactional
    public void saveAll(String filePath) {
        var contacts = contactReader.readFromFile(Paths.get(filePath));
        contactDao.saveAll(contacts);
    }

    @Transactional
    public void deleteContactFromCurrentUser(Long contactId) {
        // 1. Get current user and contact
        User user = getCurrentUser();
        Contact contact = contactDao.getContact(contactId);

        // 2. Verify ownership
        if (!user.getContacts().contains(contact)) {
            throw new AccessDeniedException("Contact not found or access denied");
        }

        // 3. Remove the relationship
        user.getContacts().remove(contact);
        contact.getUsers().remove(user);

        // 4. Update the user first to remove the relationship
        userService.updateUser(user.getId(), user);

        // 5. If no more users reference this contact, delete it
        if (contact.getUsers().isEmpty()) {
            contactDao.deleteContact(contact.getId());
        }
    }

    @Transactional
    public ContactDto updateContactForCurrentUser(Long contactId, Contact newData) {
        // 1. Get current user
        User currentUser = getCurrentUser();

        // 2. Get existing contact
        Contact existingContact = contactDao.getContact(contactId);

        // 3. Verify ownership
        if (!currentUser.getContacts().contains(existingContact)) {
            throw new AccessDeniedException("Contact does not belong to the current user");
        }

        // 4. Create a copy of the contact for the current user
        Contact savedContact = new Contact();
        savedContact.setName(newData.getName());
        savedContact.setSurname(newData.getSurname());
        savedContact.setEmail(newData.getEmail());
        savedContact.setPhoneNumber(newData.getPhoneNumber());

        // 5. Save the new contact
        long savedContactId = contactDao.addContact(savedContact);
        savedContact.setId(savedContactId);

        // 6. Update relationships
        existingContact.getUsers().remove(currentUser);
        currentUser.getContacts().remove(existingContact);
        currentUser.addContact(savedContact);

        // 7. Save the user to update the relationship
        userService.updateUser(currentUser.getId(), currentUser);

        // 8. If the old contact has no more users, delete it
        if (existingContact.getUsers().isEmpty()) {
            contactDao.deleteContact(existingContact.getId());
        }

        // 9. Return the DTO of the new contact
        return new ContactDto(savedContact);
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        return userService.getUserByUsername(username);
    }
}

