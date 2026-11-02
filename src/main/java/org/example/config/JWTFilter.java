package org.example.config;

import com.auth0.jwt.exceptions.JWTVerificationException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.service.JWTUtil;
import org.example.service.TokenBlacklistService;
import org.example.service.UserDetailsServiceImpl;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.example.exceptions.TokenException;
import org.example.exceptions.UserNotFoundException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class JWTFilter extends OncePerRequestFilter {

    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JWTUtil jwtUtil;
    private final UserDetailsServiceImpl userDetailsService;
    private final TokenBlacklistService tokenBlacklistService;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        // Пропускаем запросы без заголовка Authorization
        String authHeader = request.getHeader(AUTH_HEADER);
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX) || authHeader.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        // Извлекаем токен из заголовка
        String jwt = authHeader.substring(BEARER_PREFIX.length());

        // Пустой токен недопустим
        if (jwt.isBlank()) {
            reject(request, response, "Invalid JWT Token: Token is empty");
            return;
        }

        String username;
        try {
            // Сначала проверяем подпись, срок действия и остальные условия валидности
            username = jwtUtil.validateTokenAndRetrieveClaim(jwt);
        } catch (JWTVerificationException e) {
            log.warn("Invalid JWT token: {}", e.getMessage());
            reject(request, response, "Invalid JWT Token: " + e.getMessage());
            return;
        }

        // Проверяем отзыв только для криптографически валидного токена
        if (tokenBlacklistService.isTokenBlacklisted(jwt)) {
            log.warn("Attempt to use revoked token");
            reject(request, response, "Token has been revoked");
            return;
        }

        try {
            // Загружаем UserDetails
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            // Создаем объект аутентификации
            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                    userDetails,
                    null, // credentials не нужны после аутентификации
                    userDetails.getAuthorities()
            );

            // Устанавливаем аутентификацию в контекст
            SecurityContextHolder.getContext().setAuthentication(authToken);
            log.debug("Authenticated user: {}", username);

        } catch (UsernameNotFoundException | UserNotFoundException e) {
            log.warn("User referenced by JWT no longer exists: {}", e.getMessage());
            reject(request, response, "User not found");
            return;
        }

        // Продолжаем цепочку фильтров
        filterChain.doFilter(request, response);
    }

    private void reject(HttpServletRequest request, HttpServletResponse response, String message)
            throws IOException {
        SecurityContextHolder.clearContext();
        jwtAuthenticationEntryPoint.commence(request, response, new TokenException(message));
    }
}
