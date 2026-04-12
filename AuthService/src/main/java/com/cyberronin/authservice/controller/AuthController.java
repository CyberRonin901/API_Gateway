package com.cyberronin.authservice.controller;

import com.cyberronin.authservice.dao.UserRepo;
import com.cyberronin.authservice.model.User;
import com.cyberronin.authservice.util.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/*
TODO: implement the following for registration checks
Username uniqueness (though DB has UNIQUE constraint)
Password complexity
Required fields (name, username, password)
*/

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final JwtUtil jwtUtil;
    private final UserRepo userRepository;
    private final PasswordEncoder passwordEncoder;

    // Constructor injection for all fields
    public AuthController(JwtUtil jwtUtil, UserRepo userRepository, PasswordEncoder passwordEncoder) {
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/user/register")
    public Mono<User> register(@RequestBody User user) {
        // Encode the password before saving
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        // Set a default role if not provided
        user.setRole("ROLE_USER");

        return userRepository.save(user);
    }

    @PostMapping("/admin/register")
    public Mono<User> registerAdmin(@RequestBody User user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setRole("ROLE_ADMIN");

        return userRepository.save(user);
    }

    @PostMapping("/login")
    public Mono<String> login(@RequestBody User loginRequest) {
        return userRepository.findUserByUsername(loginRequest.getUsername())
                .filter(user -> passwordEncoder.matches(loginRequest.getPassword(), user.getPassword()))
                .map(user -> jwtUtil.generateToken(user.getUsername(), user.getId(), user.getRole()))
                // If user not found or password doesn't match, throw 401
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid Credentials")));
    }
}