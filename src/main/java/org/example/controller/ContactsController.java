package org.example.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.dto.AdminContactDto;
import org.example.dto.AppErrorResponse;
import org.example.exceptions.InvalidCsvException;
import org.example.service.ContactService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/contacts")
@Tag(name = "Contacts Admin", description = "Endpoints for managing contacts")
@SecurityRequirement(name = "bearerAuth")
public class ContactsController {
    private final ContactService contactService;

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @Operation(
        summary = "Get all contacts",
        description = "Retrieves all personal contacts with their owner IDs. Requires ADMIN role.",
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "Successfully retrieved list of contacts",
                content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    array = @ArraySchema(schema = @Schema(implementation = AdminContactDto.class))
                )
            ),
            @ApiResponse(
                responseCode = "401",
                description = "Missing or invalid JWT, or insufficient role",
                content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = AppErrorResponse.class)
                )
            )
        }
    )
    public List<AdminContactDto> getAllContacts() {
        return contactService.getAllContacts();
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_USER')")
    @Operation(
        summary = "Import contacts from CSV file",
        description = "Uploads a CSV file and imports its contacts into the current user's address book. "
                + "Requires USER or ADMIN role.",
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "Contacts imported successfully (empty body)"
            ),
            @ApiResponse(
                responseCode = "400",
                description = "Empty file, invalid CSV content or unreadable upload",
                content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = AppErrorResponse.class)
                )
            ),
            @ApiResponse(
                responseCode = "401",
                description = "Missing or invalid JWT, or insufficient role",
                content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = AppErrorResponse.class)
                )
            ),
            @ApiResponse(
                responseCode = "413",
                description = "Uploaded file exceeds the configured size limit",
                content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = AppErrorResponse.class)
                )
            )
        }
    )
    @ResponseStatus(HttpStatus.OK)
    public void saveAll(
        @Parameter(
            description = "CSV file with rows in the format name,surname,email,phoneNumber",
            required = true
        )
        @RequestPart("file") MultipartFile file
    ) {
        try {
            contactService.saveAll(file.getInputStream());
        } catch (IOException e) {
            throw new InvalidCsvException("Unable to read uploaded CSV file", e);
        }
    }
}
