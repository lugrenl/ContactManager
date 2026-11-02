package org.example.config;

import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import jakarta.servlet.FilterChain;
import org.example.exceptions.TokenException;
import org.example.service.JWTUtil;
import org.example.service.TokenBlacklistService;
import org.example.service.UserDetailsServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JWTFilterTest {

    private static final String TOKEN = "header.payload.signature";

    @Mock
    private JWTUtil jwtUtil;

    @Mock
    private UserDetailsServiceImpl userDetailsService;

    @Mock
    private TokenBlacklistService tokenBlacklistService;

    @Mock
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JWTFilter jwtFilter;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + TOKEN);
        response = new MockHttpServletResponse();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void validNonRevokedTokenAuthenticatesAndChecksRevocation() throws Exception {
        when(jwtUtil.validateTokenAndRetrieveClaim(TOKEN)).thenReturn("testuser");
        when(tokenBlacklistService.isTokenBlacklisted(TOKEN)).thenReturn(false);
        UserDetails userDetails = mock(UserDetails.class);
        when(userDetails.getAuthorities())
                .thenAnswer(invocation -> List.of(new SimpleGrantedAuthority("ROLE_USER")));
        when(userDetails.getUsername()).thenReturn("testuser");
        when(userDetailsService.loadUserByUsername("testuser")).thenReturn(userDetails);

        jwtFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(tokenBlacklistService).isTokenBlacklisted(TOKEN);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo("testuser");
    }

    @Test
    void validRevokedTokenIsRejectedBeforeUserIsLoaded() throws Exception {
        when(jwtUtil.validateTokenAndRetrieveClaim(TOKEN)).thenReturn("testuser");
        when(tokenBlacklistService.isTokenBlacklisted(TOKEN)).thenReturn(true);

        jwtFilter.doFilterInternal(request, response, filterChain);

        verifyNoInteractions(userDetailsService);
        verify(filterChain, never()).doFilter(any(), any());
        verify(jwtAuthenticationEntryPoint).commence(eq(request), eq(response), any(TokenException.class));
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void tokenWithInvalidSignatureDoesNotReachRevocationStorage() throws Exception {
        when(jwtUtil.validateTokenAndRetrieveClaim(TOKEN))
                .thenThrow(new JWTVerificationException("invalid signature"));

        jwtFilter.doFilterInternal(request, response, filterChain);

        verifyNoInteractions(tokenBlacklistService, userDetailsService);
        verify(filterChain, never()).doFilter(any(), any());
        verify(jwtAuthenticationEntryPoint).commence(eq(request), eq(response), any(TokenException.class));
    }

    @Test
    void expiredTokenDoesNotReachRevocationStorage() throws Exception {
        when(jwtUtil.validateTokenAndRetrieveClaim(TOKEN))
                .thenThrow(new TokenExpiredException("expired", Instant.now().minusSeconds(60)));

        jwtFilter.doFilterInternal(request, response, filterChain);

        verifyNoInteractions(tokenBlacklistService, userDetailsService);
        verify(filterChain, never()).doFilter(any(), any());
        verify(jwtAuthenticationEntryPoint).commence(eq(request), eq(response), any(TokenException.class));
    }

    @Test
    void malformedTokenDoesNotReachRevocationStorage() throws Exception {
        when(jwtUtil.validateTokenAndRetrieveClaim(TOKEN))
                .thenThrow(new JWTDecodeException("malformed token"));

        jwtFilter.doFilterInternal(request, response, filterChain);

        verifyNoInteractions(tokenBlacklistService, userDetailsService);
        verify(filterChain, never()).doFilter(any(), any());
        verify(jwtAuthenticationEntryPoint).commence(eq(request), eq(response), any(TokenException.class));
    }

    @Test
    void tokenOfDeletedUserIsRejected() throws Exception {
        when(jwtUtil.validateTokenAndRetrieveClaim(TOKEN)).thenReturn("deleteduser");
        when(tokenBlacklistService.isTokenBlacklisted(TOKEN)).thenReturn(false);
        when(userDetailsService.loadUserByUsername("deleteduser"))
                .thenThrow(new UsernameNotFoundException("deleteduser"));

        jwtFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain, never()).doFilter(any(), any());
        verify(jwtAuthenticationEntryPoint).commence(eq(request), eq(response), any(TokenException.class));
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void revocationStorageFailureIsNotMasked() throws Exception {
        when(jwtUtil.validateTokenAndRetrieveClaim(TOKEN)).thenReturn("testuser");
        when(tokenBlacklistService.isTokenBlacklisted(anyString()))
                .thenThrow(new DataAccessResourceFailureException("database unavailable"));

        assertThatThrownBy(() -> jwtFilter.doFilterInternal(request, response, filterChain))
                .isInstanceOf(DataAccessResourceFailureException.class);

        verify(filterChain, never()).doFilter(any(), any());
        verify(jwtAuthenticationEntryPoint, never()).commence(any(), any(), any(TokenException.class));
    }

    @Test
    void requestWithoutAuthorizationHeaderSkipsTokenChecks() throws Exception {
        MockHttpServletRequest requestWithoutToken = new MockHttpServletRequest();

        jwtFilter.doFilterInternal(requestWithoutToken, response, filterChain);

        verifyNoInteractions(jwtUtil, tokenBlacklistService, userDetailsService);
        verify(filterChain).doFilter(requestWithoutToken, response);
    }
}
