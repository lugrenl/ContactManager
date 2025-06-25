package org.example.controller;

import jakarta.validation.Valid;
import org.example.dto.ContactDto;
import org.example.service.ContactService;
import org.example.model.Contact;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/contacts")
public class ContactController {
    private final ContactService contactService;
    private final ModelMapper modelMapper;

    @Autowired
    public ContactController(ContactService contactService, ModelMapper modelMapper) {
        this.contactService = contactService;
        this.modelMapper = modelMapper;
    }

    @PostMapping
    @PreAuthorize("hasRole('ROLE_ADMIN') and hasRole('ROLE_USER')")
    public long addContact(@RequestBody @Valid ContactDto contactDto) {
        Contact contact = modelMapper.map(contactDto, Contact.class);
        return contactService.addContact(contact);
    }

    @GetMapping("/{contactId}")
    @PreAuthorize("hasRole('ROLE_ADMIN') and hasRole('ROLE_USER')")
    public ContactDto getContact(@PathVariable("contactId") long contactId) {
        return contactService.getContact(contactId);
    }

    @GetMapping
    @PreAuthorize("hasRole('ROLE_ADMIN') and hasRole('ROLE_USER')")
    public List<ContactDto> getAllContacts() {
        return contactService.getAllContacts();
    }

    @PutMapping("/{contactId}")
    @PreAuthorize("hasRole('ROLE_ADMIN') and hasRole('ROLE_USER')")
    public ContactDto updateContact(@PathVariable("contactId") long contactId,
                                    @RequestBody @Valid ContactDto contactDto) {
        Contact contact = modelMapper.map(contactDto, Contact.class);
        return contactService.updateContact(contactId, contact);
    }

    @DeleteMapping("/{contactId}")
    @PreAuthorize("hasRole('ROLE_ADMIN') and hasRole('ROLE_USER')")
    public void deleteContact(@PathVariable("contactId") long contactId) {
        contactService.deleteContact(contactId);
    }

    @PostMapping("/import")
    @PreAuthorize("hasRole('ROLE_ADMIN') and hasRole('ROLE_USER')")
    public void saveAll (@RequestParam("filePath") String filePath) {
        contactService.saveAll(filePath);
    }
}
