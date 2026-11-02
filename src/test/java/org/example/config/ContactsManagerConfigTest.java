package org.example.config;

import org.example.entity.User;
import org.example.service.RegistrationService;
import org.example.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContactsManagerConfigTest {

    private static final String CONFIGURED_PASSWORD = "configured-admin-password-123";

    @Mock
    private RegistrationService registrationService;

    @Mock
    private UserService userService;

    private ContactsManagerConfig config;

    @BeforeEach
    void setUp() {
        config = new ContactsManagerConfig(registrationService, userService);
    }

    @Test
    void doesNotCreateDefaultAdminWhenConfigurationIsMissing() {
        setAdminFields("", "", null);

        assertThat(config.createAdminUser()).isNull();
        verifyNoInteractions(userService, registrationService);
    }

    @Test
    void doesNotCreateDefaultAdminWhenOnlyUsernameIsConfigured() {
        setAdminFields("root", "", null);

        assertThatThrownBy(() -> config.createAdminUser())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("incomplete");
        verifyNoInteractions(registrationService);
    }

    @Test
    void doesNotCreateDefaultAdminWhenOnlyPasswordIsConfigured() {
        setAdminFields("", CONFIGURED_PASSWORD, null);

        assertThatThrownBy(() -> config.createAdminUser())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("incomplete");
        verifyNoInteractions(registrationService);
    }

    @Test
    void leavesExistingAdminUnchanged() {
        User existing = new User();
        existing.setId(1L);
        existing.setName("root");
        when(userService.findUserByUsername("root")).thenReturn(Optional.of(existing));
        setAdminFields("root", CONFIGURED_PASSWORD, "root@example.com");

        User result = config.createAdminUser();

        assertThat(result).isSameAs(existing);
        verify(registrationService, never()).register(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void createsConfiguredAdminWithAdminRole() {
        when(userService.findUserByUsername("root")).thenReturn(Optional.empty());
        setAdminFields("root", CONFIGURED_PASSWORD, "root@example.com");

        User result = config.createAdminUser();

        assertThat(result.getName()).isEqualTo("root");
        assertThat(result.getPassword()).isEqualTo(CONFIGURED_PASSWORD);
        assertThat(result.getEmail()).isEqualTo("root@example.com");
        assertThat(result.getRole()).isEqualTo("ROLE_ADMIN");
        verify(registrationService).register(result);
    }

    private void setAdminFields(String username, String password, String email) {
        ReflectionTestUtils.setField(config, "adminUsername", username);
        ReflectionTestUtils.setField(config, "adminPassword", password);
        ReflectionTestUtils.setField(config, "adminEmail", email);
    }
}
