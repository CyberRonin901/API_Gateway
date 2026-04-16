package com.cyberronin.authservice.controller;

import com.cyberronin.authservice.dto.TokenResponseDTO;
import com.cyberronin.authservice.dto.UserRequestDTO;
import com.cyberronin.authservice.dto.UserResponseDTO;
import com.cyberronin.authservice.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final AuthService authService; // Move logic here

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/user/register")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<UserResponseDTO> register(@Valid @RequestBody UserRequestDTO user) {
        return authService.registerUser(user, "ROLE_USER");
    }

    @PostMapping("/admin/register")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<UserResponseDTO> registerAdmin(@Valid @RequestBody UserRequestDTO user) {
        return authService.registerUser(user, "ROLE_ADMIN");
    }

    @PostMapping("/login")
    public Mono<TokenResponseDTO> login(@Valid @RequestBody UserRequestDTO user) {
        return authService.authenticate(user);
    }
}