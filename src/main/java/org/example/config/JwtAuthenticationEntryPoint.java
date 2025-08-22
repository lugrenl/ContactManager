package org.example.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.dto.AppErrorResponse;
import org.example.exceptions.TokenException;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public JwtAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, 
                        HttpServletResponse response,
                        AuthenticationException authException) throws IOException, ServletException {
        
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        
        AppErrorResponse errorResponse = new AppErrorResponse(
                "Unauthorized: " + authException.getMessage(),
                System.currentTimeMillis()
        );
        
        response.getOutputStream()
                .println(objectMapper.writeValueAsString(errorResponse));
    }
    
    public void commence(HttpServletRequest request,
                        HttpServletResponse response,
                        TokenException ex) throws IOException {
        
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        
        AppErrorResponse errorResponse = new AppErrorResponse(
                ex.getMessage(),
                System.currentTimeMillis()
        );
        
        response.getOutputStream()
                .println(objectMapper.writeValueAsString(errorResponse));
    }
}
