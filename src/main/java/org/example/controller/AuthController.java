package org.example.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.dto.AuthenticationDto;
import org.example.dto.RegistrationDto;
import org.example.entity.User;
import org.example.service.JWTUtil;
import org.example.service.RegistrationService;
import org.example.util.UserValidator;
import org.modelmapper.ModelMapper;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final RegistrationService registrationService;
    private final UserValidator userValidator;
    private final JWTUtil jwtUtil;
    private final ModelMapper modelMapper;
    private final AuthenticationManager authenticationManager;

    @PostMapping("/register")
    public Map<String, String> performRegistration(@RequestBody @Valid RegistrationDto registrationDto,
                                                   BindingResult bindingResult) {
        User user = convertToUser(registrationDto);
        userValidator.validate(user, bindingResult);

        if (bindingResult.hasErrors()) {
            return Map.of("message", "Ошибка!");
        }

        registrationService.register(user);

        String token = jwtUtil.generateToken(user.getName());
        return Map.of("jwt-token", token);
    }

    @PostMapping("/login")
    public Map<String, String> performLogin(@RequestBody AuthenticationDto authenticationDTO) {
        UsernamePasswordAuthenticationToken authInputToken =
                new UsernamePasswordAuthenticationToken(authenticationDTO.getName(),
                        authenticationDTO.getPassword());

        try {
            authenticationManager.authenticate(authInputToken);
        } catch (BadCredentialsException e) {
            return Map.of("message", "Incorrect credentials!");
        }

        String token = jwtUtil.generateToken(authenticationDTO.getName());
        return Map.of("jwt-token", token);
    }

    public User convertToUser(RegistrationDto registrationDto) {
        return this.modelMapper.map(registrationDto, User.class);
    }
}

