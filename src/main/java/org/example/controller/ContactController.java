package org.example.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.dto.RequestContactDto;
import org.example.dto.ResponseContactDto;
import org.example.entity.Contact;
import org.example.service.ContactService;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/contacts")
@Tag(name = "Contacts", description = "APIs for managing contacts")
@SecurityRequirement(name = "bearerAuth")
public class ContactController {
    private final ContactService contactService;
    private final ModelMapper modelMapper;

    @Operation(summary = "Add a new contact", description = "Creates a new contact for the current user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Contact created successfully",
                    content = @Content(schema = @Schema(implementation = ResponseContactDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_USER')")
    public ResponseEntity<ResponseContactDto> addContact(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Contact details to add",
                    required = true,
                    content = @Content(schema = @Schema(implementation = RequestContactDto.class)))
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
            ResponseContactDto savedContact = contactService.addContactToCurrentUser(contact);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedContact);
        }
    }

    @Operation(summary = "Get contact by ID", description = "Retrieves a specific contact by its ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Contact found",
                    content = @Content(schema = @Schema(implementation = ResponseContactDto.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Contact not found")
    })
    @GetMapping(value = "/{contactId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_USER')")
    public ResponseEntity<ResponseContactDto> getContact(
            @io.swagger.v3.oas.annotations.Parameter(description = "ID of the contact to retrieve", required = true)
            @PathVariable("contactId") long contactId) {
        ResponseContactDto contact = contactService.getContact(contactId);
        return ResponseEntity.ok().body(contact);
    }

    @Operation(summary = "Get all contacts", description = "Retrieves all contacts for the current user")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved contacts",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ResponseContactDto.class))))
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_USER')")
    public Set<ResponseContactDto> getAllContactsForCurrentUser() {
        return contactService.getContactsForCurrentUser();
    }

    @Operation(summary = "Update contact", description = "Updates an existing contact")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Contact updated successfully",
                    content = @Content(schema = @Schema(implementation = ResponseContactDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Contact not found")
    })
    @PutMapping(value = "/{contactId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_USER')")
    public ResponseEntity<ResponseContactDto> updateContact(
            @io.swagger.v3.oas.annotations.Parameter(description = "ID of the contact to update", required = true)
            @PathVariable("contactId") long contactId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Updated contact details",
                    required = true,
                    content = @Content(schema = @Schema(implementation = RequestContactDto.class)))
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
            return ResponseEntity.ok().body(updatedContact);
        }
    }

    @Operation(summary = "Delete contact", description = "Deletes a contact by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Contact deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Contact not found")
    })
    @DeleteMapping("/{contactId}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_USER')")
    public ResponseEntity<Void> deleteContact(
            @io.swagger.v3.oas.annotations.Parameter(description = "ID of the contact to delete", required = true)
            @PathVariable("contactId") long contactId) {
        contactService.deleteContactFromCurrentUser(contactId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
