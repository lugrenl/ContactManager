package org.example.controller;

import org.example.facade.ContactFacade;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/contacts")
public class ContactController {
    private final ContactFacade contactFacade;

    @Autowired
    public ContactController(ContactFacade contactFacade) {
        this.contactFacade = contactFacade;
    }

    @PostMapping
    public ContactDto addContact(
            @RequestParam("name") String name,
            @RequestParam("surname") String surname,
            @RequestParam("email") String email,
            @RequestParam("phoneNumber") String phoneNumber
    ) {
        return contactFacade.addContact(name, surname, email, phoneNumber);
    }

    @GetMapping("/{contactId}")
    public ContactDto getContact(@PathVariable("contactId") long contactId) {
        return contactFacade.getContact(contactId);
    }

    @GetMapping
    public List<ContactDto> getAllContacts() { return contactFacade.getAllContacts(); }

    @PutMapping("/{contactId}/{name}/{surname}/{email}/{phoneNumber}")
    public ContactDto updateContact(
            @PathVariable("contactId") long contactId,
            @PathVariable("name") String name,
            @PathVariable("surname") String surname,
            @PathVariable("email") String email,
            @PathVariable("phoneNumber") String phoneNumber
    ) {
        return contactFacade.updateContact(contactId, name, surname, email, phoneNumber);
    }

    @PutMapping("/email/{contactId}/{email}")
    public void updateEmail(
            @PathVariable("contactId") long contactId,
            @PathVariable("email") String email
    ) {
        contactFacade.updateEmail(contactId, email);
    }

    @PutMapping("/phone/{contactId}/{phoneNumber}")
    public void updatePhoneNumber(
            @PathVariable("contactId") long contactId,
            @PathVariable("phoneNumber") String phoneNumber
    ) {
        contactFacade.updatePhoneNumber(contactId, phoneNumber);
    }

    @DeleteMapping("/delete/{contactId}")
    public void deleteContact(@PathVariable("contactId") long contactId) { contactFacade.deleteContact(contactId); }

    @PostMapping("/import")
    public void saveAll (@RequestParam("filePath") String filePath) { contactFacade.saveAll(filePath); }
}
