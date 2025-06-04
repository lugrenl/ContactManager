package org.example.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users")
public class User {

    @Id
    @Column(name = "user_id")
    @NotNull(message = "Id should not be null")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @NotBlank(message = "Username is required")
    @NotEmpty(message = "Username should not be empty")
    @Size(min = 2, max = 20, message = "Username should be between 2 and 20 characters")
    @Column(name = "name")
    private String name;

    @NotBlank(message = "Password is required")
    @NotEmpty(message = "Password should not be empty")
    @Size(min = 8, max = 20, message = "Password should be between 8 and 20 characters")
    @Column(name = "password")
    private String password;

    @NotBlank(message = "User email is required")
    @NotEmpty(message = "User email should not be empty")
    @Email(message = "User email should be valid")
    @Column(name = "email")
    private String email;

    @NotBlank(message = "Role is required")
    @NotEmpty(message = "Role should not be empty")
    @Column(name = "role")
    private String role;

    @ManyToMany
    @JoinTable(
            name = "user_contacts",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "contact_id")
    )
    private Set<Contact> contacts = new HashSet<>();
}
