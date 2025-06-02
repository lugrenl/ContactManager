package org.example.controller;

import org.example.dto.ContactDto;
import org.example.facade.ContactFacade;
import org.example.model.Contact;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/contacts")
public class ContactController {
    private final ContactFacade contactFacade;
    private final ModelMapper modelMapper;

    @Autowired
    public ContactController(ContactFacade contactFacade, ModelMapper modelMapper) {
        this.contactFacade = contactFacade;
        this.modelMapper = modelMapper;
    }

    // Возвращает id добавленного контакта
    @PostMapping
    public long addContact(@RequestBody ContactDto contactDto) {
        Contact contact = convertToContact(contactDto);
        return contactFacade.addContact(contact);
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

    private Contact convertToContact(ContactDto contactDto) {
        return this.modelMapper.map(contactDto, Contact.class);
    }
}
