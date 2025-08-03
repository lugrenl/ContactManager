package org.example.controller;

import lombok.RequiredArgsConstructor;
import org.example.dto.ResponseContactDto;
import org.example.service.ContactService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/contacts")
public class ContactsController {
    private final ContactService contactService;

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public List<ResponseContactDto> getAllContacts() {
        return contactService.getAllContacts();
    }

    @PostMapping("/import")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_USER')")
    public void saveAll (@RequestParam("filePath") String filePath) {
        contactService.saveAll(filePath);
    }
}
