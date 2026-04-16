package com.cyberronin.authservice.service;

import com.cyberronin.authservice.dao.UserRepo;
import com.cyberronin.authservice.dto.TokenResponseDTO;
import com.cyberronin.authservice.dto.UserRequestDTO;
import com.cyberronin.authservice.dto.UserResponseDTO;
import com.cyberronin.authservice.exceptions.UsernameAlreadyExistsException;
import com.cyberronin.authservice.model.User;
import com.cyberronin.authservice.util.JwtUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

@Service
public class AuthService {
    private final UserRepo userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Value("${JWT_EXPIRATION}")
    private Long jwtExpiration;

    public AuthService(UserRepo userRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    public Mono<UserResponseDTO> registerUser(UserRequestDTO request, String role) {
        // 1. Map DTO to Entity immediately
        User user = new User();
        user.setName(request.name());
        user.setUsername(request.username());
        // 2. Hash the raw password from the DTO
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(role);

        return userRepository.save(user)
                .map(this::mapToResponseDTO)
                .onErrorResume(ex -> {
                    if (ex.getMessage() != null && ex.getMessage().contains("users_username_key")) {
                        return Mono.error(new UsernameAlreadyExistsException(request.username()));
                    }
                    return Mono.error(ex);
                });
    }

    public Mono<TokenResponseDTO> authenticate(UserRequestDTO request) {
        // authenticate only needs username and password from the DTO
        return userRepository.findUserByUsername(request.username())
                .filter(u -> passwordEncoder.matches(request.password(), u.getPassword()))
                .map(u -> {
                    String token = jwtUtil.generateToken(u.getUsername(), u.getId(), u.getRole());
                    return new TokenResponseDTO(token, jwtExpiration);
                })
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid Credentials")));
    }

    private UserResponseDTO mapToResponseDTO(User user) {
        return new UserResponseDTO(
                user.getUsername(),
                user.getName(),
                user.getRole(),
                java.time.LocalDateTime.now() // Ideally user.getCreatedAt() if auditing is enabled
        );
    }
}