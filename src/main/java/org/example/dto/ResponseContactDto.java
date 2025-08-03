package org.example.dto;

import jakarta.persistence.Id;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.entity.Contact;

import com.fasterxml.jackson.annotation.JsonProperty;


@Data
@NoArgsConstructor
public class ResponseContactDto {

    @Id
    @NotNull(message = "Id should not be null")
    private long id;

    @NotBlank(message = "Name is required")
    @NotEmpty(message = "Name should not be empty")
    @Size(min = 2, max = 20, message = "Name should be between 2 and 20 characters")
    @JsonProperty("name")
    private  String name;

    @NotBlank(message = "Surname is required")
    @NotEmpty(message = "Surname should not be empty")
    @Size(min = 2, max = 30, message = "Surname should be between 2 and 30 characters")
    @JsonProperty("surname")
    private String surname;

    @Email(message = "Email should be valid")
    @JsonProperty("email")
    private String email;

    @Size(min = 2, max = 18, message = "Phone number should be between 2 and 18 characters")
    @JsonProperty("phoneNumber")
    private String phoneNumber;

    public ResponseContactDto(Contact contact) {
        this.id = contact.getId();
        this.name = contact.getName();
        this.surname = contact.getSurname();
        this.email = contact.getEmail();
        this.phoneNumber = contact.getPhoneNumber();
    }
}
