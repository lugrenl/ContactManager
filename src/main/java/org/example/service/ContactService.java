package org.example.service;

import lombok.RequiredArgsConstructor;
import org.example.dao.ContactDao;
import org.example.dto.AdminContactDto;
import org.example.dto.ResponseContactDto;
import org.example.entity.Contact;
import org.example.entity.User;
import org.example.exceptions.ContactAccessDeniedException;
import org.example.exceptions.ContactAlreadyExistsException;
import org.example.util.ContactReader;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
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
        User user = getCurrentUser();

        Optional<Contact> existingContact = contactDao.findExistingContact(
                user.getId(),
                contact.getName(),
                contact.getSurname(),
                contact.getEmail(),
                contact.getPhoneNumber()
        );

        if (existingContact.isPresent()) {
            return new ResponseContactDto(existingContact.get());
        }

        user.addContact(contact);
        long contactId = contactDao.addContact(contact);
        contact.setId(contactId);
        return new ResponseContactDto(contact);
    }

    @Transactional(readOnly = true)
    public ResponseContactDto getContact(long contactId) {
        User user = getCurrentUser();
        return new ResponseContactDto(getOwnedContact(contactId, user));
    }

    @Transactional(readOnly = true)
    public List<AdminContactDto> getAllContacts() {
        return contactDao.getAllContacts().stream().map(AdminContactDto::new).toList();
    }

    @Transactional(readOnly = true)
    public Set<ResponseContactDto> getContactsForCurrentUser() {
        User user = getCurrentUser();
        return user.getContacts().stream().map(ResponseContactDto::new).collect(Collectors.toSet());
    }

    @Transactional
    public List<ResponseContactDto> saveAll(InputStream inputStream) {
        var contacts = contactReader.readFromStream(inputStream);
        return contacts.stream()
                .map(this::addContactToCurrentUser)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteContactFromCurrentUser(Long contactId) {
        User user = getCurrentUser();
        Contact contact = getOwnedContact(contactId, user);
        user.removeContact(contact);
    }

    @Transactional
    public ResponseContactDto updateContactForCurrentUser(Long contactId, Contact newData) {
        User currentUser = getCurrentUser();
        Contact existingContact = getOwnedContact(contactId, currentUser);

        Optional<Contact> duplicate = contactDao.findExistingContact(
                currentUser.getId(),
                newData.getName(),
                newData.getSurname(),
                newData.getEmail(),
                newData.getPhoneNumber()
        );
        if (duplicate.isPresent() && duplicate.get().getId() != existingContact.getId()) {
            throw new ContactAlreadyExistsException(
                    "Another contact with the same data already exists in the current user's address book");
        }

        Contact updatedContact = contactDao.updateContact(contactId, newData);
        return new ResponseContactDto(updatedContact);
    }

    private Contact getOwnedContact(long contactId, User user) {
        Contact contact = contactDao.getContact(contactId);
        if (contact.getOwner() == null || contact.getOwner().getId() != user.getId()) {
            throw new ContactAccessDeniedException("Contact does not belong to the current user");
        }
        return contact;
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        return userService.getUserByUsername(username);
    }
}
