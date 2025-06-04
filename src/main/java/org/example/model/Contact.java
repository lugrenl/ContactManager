package org.example.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "contacts")
public class Contact {

    @Id
    @NotNull(message = "Id should not be null")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "contact_id")
    private long id;

    @NotBlank(message = "Name is required")
    @NotEmpty(message = "Name should not be empty")
    @Size(min = 2, max = 20, message = "Name should be between 2 and 20 characters")
    @Column(name = "name")
    private String name;

    @NotBlank(message = "Surname is required")
    @NotEmpty(message = "Surname should not be empty")
    @Size(min = 2, max = 30, message = "Surname should be between 2 and 30 characters")
    @Column(name = "surname")
    private String surname;

    @Email(message = "Email should be valid")
    @Column(name = "email")
    private String email;

    @Size(min = 2, max = 18, message = "Phone number should be between 2 and 18 characters")
    @Column(name = "phone_number")
    private String phoneNumber;

    @ManyToMany(mappedBy = "contacts", fetch = FetchType.EAGER)
    private Set<User> users = new HashSet<>();

    public Contact(String name, String surname, String email, String phoneNumber) {
        this.name = name;
        this.surname = surname;
        this.email = email;
        this.phoneNumber = phoneNumber;
    }

    public Contact(long id, String name, String surname, String email, String phoneNumber) {
        this.id = id;
        this.name = name;
        this.surname = surname;
        this.email = email;
        this.phoneNumber = phoneNumber;
    }
}
