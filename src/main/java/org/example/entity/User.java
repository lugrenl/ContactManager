package org.example.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.Hibernate;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@ToString(exclude = "contacts")
@Entity
@Table(name = "users")
public class User {

    @Id
    @Column(name = "user_id")
    @NotNull(message = "Id should not be null")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @NotBlank(message = "Username is required")
    @NotEmpty(message = "Username should not be empty")
    @Size(min = 2, max = 20, message = "Username should be between 2 and 20 characters")
    @Column(name = "name", unique = true)
    private String name;

    @NotBlank(message = "Password is required")
    @NotEmpty(message = "Password should not be empty")
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

    @OneToMany(mappedBy = "owner", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Contact> contacts = new HashSet<>();

    public void addContact(Contact contact) {
        contact.setOwner(this);
        contacts.add(contact);
    }

    public void removeContact(Contact contact) {
        contacts.remove(contact);
        if (this.equals(contact.getOwner())) {
            contact.setOwner(null);
        }
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) {
            return false;
        }
        User user = (User) other;
        return getId() != 0 && getId() == user.getId();
    }

    @Override
    public int hashCode() {
        return Hibernate.getClass(this).hashCode();
    }
}
