package org.example.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.entity.Contact;

import com.fasterxml.jackson.annotation.JsonProperty;

@Data
@NoArgsConstructor
@Schema(description = "DTO for creating or updating a contact")
public class RequestContactDto {

    @Schema(description = "Contact's first name", example = "John", required = true)
    @NotBlank(message = "Name is required")
    @NotEmpty(message = "Name should not be empty")
    @Size(min = 2, max = 20, message = "Name should be between 2 and 20 characters")
    @JsonProperty("name")
    private String name;

    @Schema(description = "Contact's last name", example = "Doe", required = true)
    @NotBlank(message = "Surname is required")
    @NotEmpty(message = "Surname should not be empty")
    @Size(min = 2, max = 30, message = "Surname should be between 2 and 30 characters")
    @JsonProperty("surname")
    private String surname;

    @Schema(description = "Contact's email address", example = "john.doe@example.com", required = false)
    @Email(message = "Email should be valid")
    @JsonProperty("email")
    private String email;

    @Schema(description = "Contact's phone number", example = "+1234567890", required = false)
    @Size(min = 2, max = 18, message = "Phone number should be between 2 and 18 characters")
    @JsonProperty("phoneNumber")
    private String phoneNumber;

    public RequestContactDto(Contact contact) {
        this.name = contact.getName();
        this.surname = contact.getSurname();
        this.email = contact.getEmail();
        this.phoneNumber = contact.getPhoneNumber();
    }
}
