package org.example.service;

import org.example.config.ContactsManagerConfig;
import org.example.dto.ContactDto;
import org.example.model.Contact;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.List;

/**
 * Unit tests for {@link ContactService}.
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = ContactsManagerConfig.class)
@Sql("classpath:contact.sql")
public class ContactServiceTests {

    private static final Contact IVAN = new Contact(
            1L, "Ivan", "Ivanov", "iivanov@gmail.com", "1234567"
    );

    private static final Contact MARIA = new Contact(
            2L, "Maria", "Ivanova", "mivanova@gmail.com", "7654321"
    );

    private final ContactService contactService;

    @Autowired
    public ContactServiceTests(ContactService contactService) {
        this.contactService = contactService;
    }

    @Test
    void saveContacts() throws IOException {
        String filePath = new ClassPathResource("contacts.csv").getFile().getPath();
        contactService.saveAll(filePath);

        List<ContactDto> contacts = contactService.getAllContacts();

        assertThat(contacts).containsExactly(new ContactDto(IVAN), new ContactDto(MARIA));
    }
}