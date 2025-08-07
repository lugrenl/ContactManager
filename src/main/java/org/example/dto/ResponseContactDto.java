package org.example.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Id;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.entity.Contact;

import com.fasterxml.jackson.annotation.JsonProperty;

@Data
@NoArgsConstructor
@Schema(description = "Response DTO containing contact information")
public class ResponseContactDto {

    @Id
    @NotNull(message = "Id should not be null")
    @Schema(description = "Unique identifier of the contact", example = "1", required = true)
    private long id;

    @NotBlank(message = "Name is required")
    @NotEmpty(message = "Name should not be empty")
    @Size(min = 2, max = 20, message = "Name should be between 2 and 20 characters")
    @JsonProperty("name")
    @Schema(description = "Contact's first name", example = "John", required = true)
    private String name;

    @NotBlank(message = "Surname is required")
    @NotEmpty(message = "Surname should not be empty")
    @Size(min = 2, max = 30, message = "Surname should be between 2 and 30 characters")
    @JsonProperty("surname")
    @Schema(description = "Contact's last name", example = "Doe", required = true)
    private String surname;

    @Email(message = "Email should be valid")
    @JsonProperty("email")
    @Schema(description = "Contact's email address", example = "john.doe@example.com", required = false)
    private String email;

    @Size(min = 2, max = 18, message = "Phone number should be between 2 and 18 characters")
    @JsonProperty("phoneNumber")
    @Schema(description = "Contact's phone number", example = "+1234567890", required = false)
    private String phoneNumber;

    public ResponseContactDto(Contact contact) {
        this.id = contact.getId();
        this.name = contact.getName();
        this.surname = contact.getSurname();
        this.email = contact.getEmail();
        this.phoneNumber = contact.getPhoneNumber();
    }
}
