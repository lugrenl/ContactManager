package org.example.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class RegistrationDto {

    @NotBlank(message = "Username is required")
    @NotEmpty(message = "Username should not be empty")
    @Size(min = 2, max = 20, message = "Username should be between 2 and 20 characters")
    @JsonProperty("name")
    private String name;

    @NotBlank(message = "Password is required")
    @NotEmpty(message = "Password should not be empty")
    //@Size(min = 8, max = 20, message = "Password should be between 8 and 20 characters")
    @JsonProperty("password")
    private String password;

    @NotBlank(message = "User email is required")
    @NotEmpty(message = "User email should not be empty")
    @Email(message = "User email should be valid")
    @JsonProperty("email")
    private String email;

    // TODO validation password
}
