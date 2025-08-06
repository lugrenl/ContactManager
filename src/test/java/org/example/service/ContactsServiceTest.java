package org.example.service;

import org.example.dao.ContactDao;
import org.example.dto.ResponseContactDto;
import org.example.entity.Contact;
import org.example.entity.User;
import org.example.util.ContactReader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.annotation.DirtiesContext;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ContactsServiceTest {

    @Mock
    private ContactDao contactDao;

    @Mock
    private ContactReader contactReader;

    @InjectMocks
    private ContactService contactService;

    private User testUser;
    private Contact testContact;

    @BeforeEach
    void setUp() {
        // Setup test user
        testUser = new User();
        testUser.setId(1L);
        testUser.setName("testuser");
        testUser.setPassword("password");
        testUser.setEmail("test@example.com");

        // Setup test contact
        testContact = new Contact();
        testContact.setId(1L);
        testContact.setName("John");
        testContact.setSurname("Doe");
        testContact.setEmail("john.doe@example.com");
        testContact.setPhoneNumber("+1234567890");
    }

    @Test
    void getAllContacts_ReturnsAllContacts() {
        // Arrange
        when(contactDao.getAllContacts()).thenReturn(List.of(testContact));

        // Act
        List<ResponseContactDto> result = contactService.getAllContacts();

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(1L);
    }

    @Test
    void saveAll_ProcessesFileSuccessfully() {
        // Arrange
        String filePath = "test.csv";
        when(contactReader.readFromFile(any())).thenReturn(List.of(testContact));

        // Act
        contactService.saveAll(filePath);

        // Assert
        verify(contactDao).saveAll(anyList());
    }
}
