package org.example.controller;

import org.example.dao.ContactDao;
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
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;

@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ContactTransactionRollbackIT {

    private static final String USERNAME = "rollbackuser";

    @Autowired
    private ContactService contactService;

    @Autowired
    private RegistrationService registrationService;

    @MockitoSpyBean
    private ContactDao contactDao;

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setName(USERNAME);
        user.setPassword("rollbackpass123");
        user.setEmail("rollbackuser@example.com");
        registrationService.register(user);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(USERNAME, null, List.of()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void failureDuringUpdateRollsBackAllChanges() {
        ResponseContactDto oldContact = contactService.addContactToCurrentUser(
                new Contact("Old", "Contact", "old@example.com", "1234567890"));
        long oldContactId = oldContact.getId();
        int contactsBefore = contactService.getAllContacts().size();

        doAnswer(invocation -> {
            invocation.callRealMethod();
            throw new IllegalStateException("forced failure");
        }).when(contactDao).updateContact(anyLong(), any(Contact.class));

        assertThatThrownBy(() -> contactService.updateContactForCurrentUser(
                oldContactId, new Contact("New", "Contact", "new@example.com", "0987654321")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("forced failure");

        assertThat(contactService.getAllContacts()).hasSize(contactsBefore);
        assertThat(contactService.getAllContacts())
                .extracting(dto -> dto.getName())
                .doesNotContain("New");
        assertThat(contactService.getContactsForCurrentUser())
                .extracting(ResponseContactDto::getId)
                .containsExactly(oldContactId)
                .isNotEmpty();
        assertThat(contactService.getContact(oldContactId).getName()).isEqualTo("Old");
    }
}