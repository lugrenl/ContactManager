package org.example.service;

import org.example.dao.UserDao;
import org.example.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserDetailsServiceImplTest {

    @Mock
    private UserDao userDao;

    @InjectMocks
    private UserDetailsServiceImpl userDetailsService;

    @Test
    void missingUserRaisesStandardAuthenticationException() {
        when(userDao.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("ghost"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("ghost");
    }

    @Test
    void existingUserIsLoaded() {
        User user = new User();
        user.setName("testuser");
        user.setPassword("hashed");
        user.setRole("ROLE_USER");
        when(userDao.findByUsername("testuser")).thenReturn(Optional.of(user));

        UserDetails result = userDetailsService.loadUserByUsername("testuser");

        assertThat(result.getUsername()).isEqualTo("testuser");
        assertThat(result.getAuthorities())
                .extracting(Object::toString)
                .containsExactly("ROLE_USER");
    }
}
