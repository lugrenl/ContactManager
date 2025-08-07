package org.example.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.entity.Contact;
import org.example.entity.User;

import java.util.Set;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@Schema(description = "Response DTO containing user information")
public class ResponseUserDto {

    @NotNull(message = "Id should not be null")
    @Schema(description = "Unique identifier of the user", example = "1", required = true)
    private long id;

    @NotBlank(message = "Username is required")
    @NotEmpty(message = "Username should not be empty")
    @Size(min = 2, max = 20, message = "Username should be between 2 and 20 characters")
    @Schema(description = "User's username", example = "johndoe", required = true)
    @JsonProperty("name")
    private String name;

    @NotBlank(message = "User email is required")
    @NotEmpty(message = "User email should not be empty")
    @Email(message = "User email should be valid")
    @Schema(description = "User's email address", example = "john.doe@example.com", 
            required = true, format = "email")
    @JsonProperty("email")
    private String email;

    @NotBlank(message = "Role is required")
    @NotEmpty(message = "Role should not be empty")
    @Schema(description = "User's role", example = "ROLE_USER", 
            allowableValues = {"ROLE_USER", "ROLE_ADMIN"}, required = true)
    @JsonProperty("role")
    private String role;

    @Schema(description = "Set of contact IDs associated with the user", 
            example = "[1, 2, 3]", required = false)
    @JsonProperty("contactIds")
    private Set<Long> contactIds;

    public ResponseUserDto(User user) {
        this.id = user.getId();
        this.name = user.getName();
        this.email = user.getEmail();
        this.role = user.getRole();
        this.contactIds = user.getContacts().stream()
                .map(Contact::getId)
                .collect(Collectors.toSet());
    }
}
