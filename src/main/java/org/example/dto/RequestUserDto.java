package org.example.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.entity.User;

@Data
@NoArgsConstructor
@Schema(description = "DTO for creating or updating a user (admin only)")
public class RequestUserDto {

    @Schema(description = "User's username", example = "johndoe", required = true, 
            minLength = 2, maxLength = 20)
    @NotBlank(message = "Username is required")
    @NotEmpty(message = "Username should not be empty")
    @Size(min = 2, max = 20, message = "Username should be between 2 and 20 characters")
    @JsonProperty("name")
    private String name;

    @Schema(description = "User's password. Required when creating a user, optional when updating.", 
            example = "securePassword123!", required = false, format = "password")
    @NotBlank(message = "Password is required")
    @NotEmpty(message = "Password should not be empty")
    @Size(min = 8, max = 20, message = "Password should be between 8 and 20 characters")
    @JsonProperty("password")
    private String password;

    @Schema(description = "User's email address", example = "john.doe@example.com", 
            required = true, format = "email")
    @NotBlank(message = "User email is required")
    @NotEmpty(message = "User email should not be empty")
    @Email(message = "User email should be valid")
    @JsonProperty("email")
    private String email;

    @Schema(description = "User's role. Default is 'ROLE_USER' if not specified.", 
            example = "ROLE_USER", allowableValues = {"ROLE_USER", "ROLE_ADMIN"}, 
            required = false)
    @JsonProperty("role")
    private String role;

    public RequestUserDto(User user) {
        this.name = user.getName();
        this.password = user.getPassword();
        this.email = user.getEmail();
        this.role = user.getRole();
    }
}
