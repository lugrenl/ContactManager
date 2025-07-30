package org.example.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class AuthenticationDto {

    @NotEmpty(message = "Имя не должно быть пустым")
    @Size(min = 2, max = 100, message = "Имя должно быть от 2 до 100 символов длиной")
    private String name;

    @NotBlank(message = "Password is required")
    @NotEmpty(message = "Password should not be empty")
    //@Size(min = 8, max = 20, message = "Password should be between 8 and 20 characters")
    private String password;

}
