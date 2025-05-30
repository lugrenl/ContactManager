package org.example.repository;

import org.example.config.ContactsManagerConfig;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.jdbc.Sql;
import org.example.model.Contact;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = ContactsManagerConfig.class)
@Sql("classpath:contact.sql")
public class ContactRepositoryTests {

    private final ContactRepository contactRepository;

    @Autowired
    public ContactRepositoryTests(ContactRepository contactRepository) {
        this.contactRepository = contactRepository;
    }

    private static final Contact IVAN = new Contact(
            1000L, "Ivan", "Ivanov", "iivanov@gmail.com", "1234567"
    );

    private static final Contact MARIA = new Contact(
            2000L, "Maria", "Ivanova", "mivanova@gmail.com", "7654321"
    );

    /**
     * There are two contacts inserted in the database in contact.sql.
     */
    private static final List<Contact> PERSISTED_CONTACTS = List.of(IVAN, MARIA);

    @Test
    void addContact() {
        var contact = new Contact("Jackie", "Chan", "jchan@gmail.com", "1234567890");
        var contactId = contactRepository.save(contact).getId();
        contact.setId(contactId);
        var contactInDb = contactRepository.findById(contactId)
                .orElseThrow(IllegalArgumentException::new);


        assertThat(contactInDb).isEqualTo(contact);
    }

    @Test
    void getContact() {
        var contact = contactRepository.findById(IVAN.getId())
                .orElseThrow(IllegalArgumentException::new);

        assertThat(contact).isEqualTo(IVAN);
    }

    @Test
    void getAllContacts() {
        var contacts = contactRepository.findAll();

        assertThat(contacts).containsAll(PERSISTED_CONTACTS);
    }

    @Test
    void updatePhoneNumber() {
        var contact = new Contact("Jekyll", "Hide", "jhide@gmail.com", "");
        var contactId = contactRepository.save(contact).getId();

        var newPhone = "777-77-77";
        contactRepository.updatePhone(contactId, newPhone);

        var updatedContact = contactRepository.findById(contactId)
                .orElseThrow(IllegalArgumentException::new);
        assertThat(updatedContact.getPhoneNumber()).isEqualTo(newPhone);
    }

    @Test
    void updateEmail() {
        var contact = new Contact("Captain", "America", "", "");
        var contactId = contactRepository.save(contact).getId();

        var newEmail = "cap@gmail.com";
        contactRepository.updateEmail(contactId, newEmail);

        var updatedContact = contactRepository.findById(contactId)
                .orElseThrow(IllegalArgumentException::new);
        assertThat(updatedContact.getEmail()).isEqualTo(newEmail);
    }

    @Test
    void deleteContact() {
        var contact = new Contact("To be", "Deleted", "", "");
        var contactId = contactRepository.save(contact).getId();

        contactRepository.deleteById(contactId);

        var deletedContact = contactRepository.findById(contactId);
        assertThat(deletedContact).isNotPresent();
    }

    @Test
    void updateContact() {
        var contact = new Contact("Name", "Surname", "email", "phonenumber");
        var contactId = contactRepository.save(contact).getId();

        var newName = "NewName";
        var newSurname = "NewSurname";
        var newEmail = "newemail";
        var newPhoneNumber = "newphonenumber";

        var NewContact = new Contact(contactId, newName, newSurname, newEmail, newPhoneNumber);
        var updatedContact = contactRepository.save(NewContact);

        assertThat(updatedContact).isEqualTo(NewContact);
    }
}
