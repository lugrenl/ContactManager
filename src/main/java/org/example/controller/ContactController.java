package org.example.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.dto.ContactDto;
import org.example.service.ContactService;
import org.example.entity.Contact;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/contacts")
public class ContactController {
    private final ContactService contactService;
    private final ModelMapper modelMapper;

    @PostMapping
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_USER')")
    public ResponseEntity<ContactDto> addContact(@RequestBody @Valid ContactDto contactDto) {
        Contact contact = modelMapper.map(contactDto, Contact.class);
        ContactDto savedContact = contactService.addContactToCurrentUser(contact);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedContact);
    }

    @GetMapping("/{contactId}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_USER')")
    public ContactDto getContact(@PathVariable("contactId") long contactId) {
        return contactService.getContact(contactId);
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public List<ContactDto> getAllContacts() {
        return contactService.getAllContacts();
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_USER')")
    public Set<ContactDto> getAllContactsForCurrentUser() {
        return contactService.getContactsForCurrentUser();
    }

    @PutMapping("/{contactId}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_USER')")
    public ResponseEntity<ContactDto> updateContact(@PathVariable("contactId") long contactId,
                                                    @RequestBody @Valid ContactDto contactDto) {
        Contact contact = modelMapper.map(contactDto, Contact.class);
        ContactDto updatedContact = contactService.updateContactForCurrentUser(contactId, contact);
        return ResponseEntity.ok(updatedContact);
    }

    @DeleteMapping("/{contactId}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_USER')")
    public ResponseEntity<?> deleteContact(@PathVariable("contactId") long contactId) {
        contactService.deleteContactFromCurrentUser(contactId);
        return ResponseEntity.ok().build();
    }

    //TODO move to separate controller
    @PostMapping("/import")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_USER')")
    public void saveAll (@RequestParam("filePath") String filePath) {
        contactService.saveAll(filePath);
    }
}
