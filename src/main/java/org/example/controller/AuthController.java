package org.example.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.dto.AppErrorResponse;
import org.example.dto.AuthenticationDto;
import org.example.dto.RegistrationDto;
import org.example.entity.User;
import org.example.exceptions.IncorrectCredentialsException;
import org.example.exceptions.TokenException;
import org.example.service.JWTUtil;
import org.example.service.RegistrationService;
import org.example.service.TokenBlacklistService;
import org.example.util.UserValidator;
import org.modelmapper.ModelMapper;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "APIs for user authentication and registration")
public class AuthController {

    private final RegistrationService registrationService;
    private final UserValidator userValidator;
    private final JWTUtil jwtUtil;
    private final ModelMapper modelMapper;
    private final AuthenticationManager authenticationManager;
    private final TokenBlacklistService tokenBlacklistService;

    @Operation(summary = "Register a new user", description = "Creates a new user account and returns a JWT token")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User registered successfully", 
                    content = @Content(mediaType = "application/json", 
                    schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "409", description = "User with this username already exists",
                    content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = AppErrorResponse.class)))
    })
    @PostMapping(value = "/register", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, String> performRegistration(@io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "User registration details", 
            required = true,
            content = @Content(schema = @Schema(implementation = RegistrationDto.class)))
            @RequestBody @Valid RegistrationDto registrationDto,
            BindingResult bindingResult) throws BindException {
        User user = convertToUser(registrationDto);
        userValidator.validate(user, bindingResult);

        if (bindingResult.hasErrors()) {
            if (bindingResult instanceof BindException exception) {
                throw exception;
            } else {
                throw new BindException(bindingResult);
            }
        } else {
            registrationService.register(user);
            String token = jwtUtil.generateToken(user.getName());
            return Map.of("jwt-token", token);
        }
    }

    @Operation(summary = "User login", description = "Authenticates a user and returns a JWT token")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Login successful", 
                    content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "401", description = "Invalid credentials",
                    content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = AppErrorResponse.class)))
    })
    @PostMapping(value = "/login", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, String> performLogin(@io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "User login credentials", 
            required = true,
            content = @Content(schema = @Schema(implementation = AuthenticationDto.class)))
            @RequestBody AuthenticationDto authenticationDTO) {

        UsernamePasswordAuthenticationToken authInputToken =
                new UsernamePasswordAuthenticationToken(authenticationDTO.getName(),
                        authenticationDTO.getPassword());

        try {
            authenticationManager.authenticate(authInputToken);
        } catch (AuthenticationException e) {
            throw new IncorrectCredentialsException("Incorrect credentials!");
        }

        String token = jwtUtil.generateToken(authenticationDTO.getName());
        return Map.of("jwt-token", token);
    }

    public User convertToUser(RegistrationDto registrationDto) {
        return this.modelMapper.map(registrationDto, User.class);
    }

    @Operation(summary = "User logout", description = "Invalidates the current user's JWT token")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Logout successful",
                    content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = String.class))),
            @ApiResponse(responseCode = "401", description = "Invalid or missing token",
                    content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = AppErrorResponse.class)))
    })
    @PostMapping(value = "/logout", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> logout(
            @io.swagger.v3.oas.annotations.Parameter(
                    description = "JWT token in the format 'Bearer {token}'",
                    required = true,
                    example = "Bearer your.jwt.token.here")
            @RequestHeader("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new TokenException("Invalid or missing Authorization header. Expected format: 'Bearer <token>'");
        }
        
        String token = authHeader.substring(7);
        if (token.isBlank()) {
            throw new TokenException("Token cannot be empty");
        }
        
        try {
            tokenBlacklistService.blacklistToken(token);
            return ResponseEntity.ok().body("Logged out successfully");
        } catch (Exception e) {
            throw new TokenException("Failed to process logout: " + e.getMessage());
        }
    }
}

