package org.example.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.entity.Contact;

@Data
@NoArgsConstructor
@Schema(description = "Administrative view of a contact, including its owner")
public class AdminContactDto {

    @Schema(description = "Unique identifier of the contact", example = "1", required = true)
    private long id;

    @Schema(description = "Contact's first name", example = "John", required = true)
    @JsonProperty("name")
    private String name;

    @Schema(description = "Contact's last name", example = "Doe", required = true)
    @JsonProperty("surname")
    private String surname;

    @Schema(description = "Contact's email address", example = "john.doe@example.com", required = false)
    @JsonProperty("email")
    private String email;

    @Schema(description = "Contact's phone number", example = "+1234567890", required = false)
    @JsonProperty("phoneNumber")
    private String phoneNumber;

    @Schema(description = "Identifier of the user who owns the contact", example = "1", required = true)
    @JsonProperty("ownerId")
    private long ownerId;

    public AdminContactDto(Contact contact) {
        this.id = contact.getId();
        this.name = contact.getName();
        this.surname = contact.getSurname();
        this.email = contact.getEmail();
        this.phoneNumber = contact.getPhoneNumber();
        this.ownerId = contact.getOwner().getId();
    }
}
