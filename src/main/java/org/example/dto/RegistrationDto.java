package org.example.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import com.fasterxml.jackson.annotation.JsonProperty;

@Setter
@Getter
@Schema(description = "DTO for user registration")
public class RegistrationDto {

    @Schema(description = "User's username", example = "johndoe", required = true, 
            minLength = 2, maxLength = 20)
    @NotBlank(message = "Username is required")
    @NotEmpty(message = "Username should not be empty")
    @Size(min = 2, max = 20, message = "Username should be between 2 and 20 characters")
    @JsonProperty("name")
    private String name;

    @Schema(description = "User's password", example = "securePassword123!", required = true,
            minLength = 8, maxLength = 20, format = "password")
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
}
