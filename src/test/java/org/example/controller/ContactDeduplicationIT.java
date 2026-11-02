package org.example.controller;

import org.example.dto.ResponseContactDto;
import org.example.entity.Contact;
import org.example.entity.User;
import org.example.service.ContactService;
import org.example.service.RegistrationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ContactDeduplicationIT {

    private static final String USERNAME = "dedupuser";

    @Autowired
    private ContactService contactService;

    @Autowired
    private RegistrationService registrationService;

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setName(USERNAME);
        user.setPassword("deduppass123");
        user.setEmail("dedupuser@example.com");
        registrationService.register(user);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(USERNAME, null, List.of()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void duplicateWithMissingEmailReusesExistingContact() {
        ResponseContactDto first = contactService.addContactToCurrentUser(
                new Contact("Alice", "Smith", null, "+1234567890"));
        ResponseContactDto second = contactService.addContactToCurrentUser(
                new Contact("Alice", "Smith", null, "+1234567890"));

        assertThat(second.getId()).isEqualTo(first.getId());
        assertThat(contactService.getAllContacts()).hasSize(1);
    }

    @Test
    void duplicateWithMissingPhoneReusesExistingContact() {
        ResponseContactDto first = contactService.addContactToCurrentUser(
                new Contact("Bob", "Brown", "bob@example.com", null));
        ResponseContactDto second = contactService.addContactToCurrentUser(
                new Contact("Bob", "Brown", "bob@example.com", null));

        assertThat(second.getId()).isEqualTo(first.getId());
        assertThat(contactService.getAllContacts()).hasSize(1);
    }

    @Test
    void duplicateWithAllOptionalFieldsMissingReusesExistingContact() {
        ResponseContactDto first = contactService.addContactToCurrentUser(
                new Contact("Carol", "White", null, null));
        ResponseContactDto second = contactService.addContactToCurrentUser(
                new Contact("Carol", "White", null, null));

        assertThat(second.getId()).isEqualTo(first.getId());
        assertThat(contactService.getAllContacts()).hasSize(1);
    }

    @Test
    void differentContactsWithMissingOptionalFieldsAreNotMerged() {
        contactService.addContactToCurrentUser(new Contact("Dave", "Green", null, null));
        contactService.addContactToCurrentUser(new Contact("Eve", "Black", null, null));

        assertThat(contactService.getAllContacts()).hasSize(2);
    }

    @Test
    void sameNameAndSurnameButDifferentEmailAreNotDuplicates() {
        contactService.addContactToCurrentUser(new Contact("Frank", "Gray", "frank1@example.com", null));
        contactService.addContactToCurrentUser(new Contact("Frank", "Gray", "frank2@example.com", null));

        assertThat(contactService.getAllContacts()).hasSize(2);
    }

    @Test
    void identicalContactsOfDifferentUsersAreIndependent() {
        ResponseContactDto first = contactService.addContactToCurrentUser(
                new Contact("Shared", "Data", "shared@example.com", "+1234567890"));

        User other = new User();
        other.setName("otherdedup");
        other.setPassword("otherpass123");
        other.setEmail("otherdedup@example.com");
        registrationService.register(other);
        authenticate("otherdedup");

        ResponseContactDto second = contactService.addContactToCurrentUser(
                new Contact("Shared", "Data", "shared@example.com", "+1234567890"));

        assertThat(second.getId()).isNotEqualTo(first.getId());
        assertThat(contactService.getContactsForCurrentUser())
                .extracting(ResponseContactDto::getId)
                .containsExactly(second.getId());
        assertThat(contactService.getAllContacts()).hasSize(2);
    }

    private void authenticate(String username) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(username, null, List.of()));
    }
}