package org.example.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.dto.ResponseContactDto;
import org.example.service.ContactService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/contacts")
@Tag(name = "Contacts Admin", description = "Endpoints for managing contacts (Admin only)")
@SecurityRequirement(name = "bearerAuth")
public class ContactsController {
    private final ContactService contactService;

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @Operation(
        summary = "Get all contacts",
        description = "Retrieves a list of all contacts in the system. Requires ADMIN role.",
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "Successfully retrieved list of contacts",
                content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    array = @ArraySchema(schema = @Schema(implementation = ResponseContactDto.class))
                )
            ),
            @ApiResponse(
                responseCode = "403",
                description = "Access denied - requires ADMIN role"
            )
        }
    )
    public List<ResponseContactDto> getAllContacts() {
        return contactService.getAllContacts();
    }

    @PostMapping("/import")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_USER')")
    @Operation(
        summary = "Import contacts from file",
        description = "Imports contacts from a file located at the specified path. Requires USER or ADMIN role.",
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "Contacts imported successfully"
            ),
            @ApiResponse(
                responseCode = "400",
                description = "Invalid file path or file format"
            ),
            @ApiResponse(
                responseCode = "403",
                description = "Access denied - requires USER or ADMIN role"
            )
        }
    )
    @ResponseStatus(HttpStatus.OK)
    public void saveAll(
        @Parameter(
            name = "filePath",
            description = "Absolute path to the file containing contacts",
            required = true,
            example = "C:/contacts/import.csv"
        )
        @RequestParam("filePath") String filePath
    ) {
        contactService.saveAll(filePath);
    }
}
