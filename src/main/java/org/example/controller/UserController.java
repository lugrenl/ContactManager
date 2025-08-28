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
import org.example.dto.AppErrorResponse;
import org.example.dto.RequestUserDto;
import org.example.dto.ResponseUserDto;
import org.example.entity.User;
import org.example.service.UserService;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.example.exceptions.UserNotFoundException;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/users")
@Tag(name = "Users", description = "APIs for managing users (Admin only)")
@SecurityRequirement(name = "bearerAuth")
public class UserController {
    private final UserService userService;
    private final ModelMapper modelMapper;

    @Operation(summary = "Get user by ID", description = "Retrieves a specific user by ID (Admin only)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User found",
                    content = @Content(schema = @Schema(implementation = ResponseUserDto.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Admin role required"),
            @ApiResponse(responseCode = "404", description = "User not found",
                    content = @Content(schema = @Schema(implementation = AppErrorResponse.class)))
    })
    @GetMapping(value = "/{userId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<ResponseUserDto> getUser(
            @io.swagger.v3.oas.annotations.Parameter(description = "ID of the user to retrieve", required = true)
            @PathVariable("userId") Long userId) {
        try {
            return ResponseEntity.ok().body(userService.getUser(userId));
        } catch (UserNotFoundException e) {
            throw new UserNotFoundException("User not found with id: " + userId);
        }
    }

    @Operation(summary = "Get all users", description = "Retrieves a list of all users (Admin only)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved users",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = ResponseUserDto.class)))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Admin role required")
    })
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public List<ResponseUserDto> getAllUsers() {
        return userService.getAllUsers();
    }

    @Operation(summary = "Update user", description = "Updates an existing user (Admin only)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User updated successfully",
                    content = @Content(schema = @Schema(implementation = ResponseUserDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Admin role required"),
            @ApiResponse(responseCode = "404", description = "User not found",
                    content = @Content(schema = @Schema(implementation = AppErrorResponse.class)))
    })
    @PutMapping(value = "/{userId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<ResponseUserDto> updateUser(
            @io.swagger.v3.oas.annotations.Parameter(description = "ID of the user to update", required = true)
            @PathVariable("userId") long userId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Updated user details",
                    required = true,
                    content = @Content(schema = @Schema(implementation = RequestUserDto.class)))
            @RequestBody @Valid RequestUserDto requestUserDto,
            BindingResult bindingResult) throws BindException {
        if (bindingResult.hasErrors()) {
            if (bindingResult instanceof BindException exception) {
                throw exception;
            } else {
                throw new BindException(bindingResult);
            }
        } else {
            User user = modelMapper.map(requestUserDto, User.class);
            return ResponseEntity.ok().body(userService.updateUser(userId, user));
        }
    }

    @Operation(summary = "Delete user", description = "Deletes a user by ID (Admin only)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "User deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Admin role required"),
            @ApiResponse(responseCode = "404", description = "User not found",
                    content = @Content(schema = @Schema(implementation = AppErrorResponse.class)))
    })
    @DeleteMapping("/{userId}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<Void> deleteUser(
            @io.swagger.v3.oas.annotations.Parameter(description = "ID of the user to delete", required = true)
            @PathVariable("userId") long userId) {
        try {
            userService.deleteUser(userId);
            return ResponseEntity.noContent().build();
        } catch (UserNotFoundException e) {
            throw new UserNotFoundException("User not found with id: " + userId);
        }
    }
}
