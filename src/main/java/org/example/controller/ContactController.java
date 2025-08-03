package org.example.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.dto.RequestContactDto;
import org.example.dto.ResponseContactDto;
import org.example.service.ContactService;
import org.example.entity.Contact;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/contacts")
public class ContactController {
    private final ContactService contactService;
    private final ModelMapper modelMapper;

    @PostMapping
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_USER')")
    public ResponseEntity<ResponseContactDto> addContact(@RequestBody @Valid RequestContactDto requestContactDto,
                                                        BindingResult bindingResult) throws BindException {
        if (bindingResult.hasErrors()) {
            if (bindingResult instanceof BindException exception) {
                throw exception;
            } else {
                throw new BindException(bindingResult);
            }
        } else {
            Contact contact = modelMapper.map(requestContactDto, Contact.class);
            ResponseContactDto savedContact = contactService.addContactToCurrentUser(contact);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedContact);
        }
    }

    @GetMapping("/{contactId}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_USER')")
    public ResponseEntity<ResponseContactDto> getContact(@PathVariable("contactId") long contactId) {
        ResponseContactDto contact = contactService.getContact(contactId);
        return ResponseEntity.ok().body(contact);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_USER')")
    public Set<ResponseContactDto> getAllContactsForCurrentUser() {
        return contactService.getContactsForCurrentUser();
    }

    @PutMapping("/{contactId}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_USER')")
    public ResponseEntity<ResponseContactDto> updateContact(@PathVariable("contactId") long contactId,
                                                            @RequestBody @Valid RequestContactDto requestContactDto,
                                                            BindingResult bindingResult) throws BindException {
        if (bindingResult.hasErrors()) {
            if (bindingResult instanceof BindException exception) {
                throw exception;
            } else {
                throw new BindException(bindingResult);
            }
        } else {
            Contact contact = modelMapper.map(requestContactDto, Contact.class);
            ResponseContactDto updatedContact = contactService.updateContactForCurrentUser(contactId, contact);
            return ResponseEntity.ok(updatedContact);
        }
    }

    @DeleteMapping("/{contactId}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_USER')")
    public ResponseEntity<?> deleteContact(@PathVariable("contactId") long contactId) {
        contactService.deleteContactFromCurrentUser(contactId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
