package com.cyberronin.authservice.service;

import com.cyberronin.authservice.dao.UserRepo;
import com.cyberronin.authservice.model.CustomUserDetails;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;

/*
ReactiveUserDetailsService -> interface provided by Spring Security to load user data.
findByUsername -> called automatically during authentication
User -> fetched from database
org.springframework.security.core.userdetails.User -> Spring Security representation of a user

new SimpleGrantedAuthority(user.getRole()) -> authorities/roles (can be extended later)
        DB stores roles as "ROLE_USER" or "ROLE_ADMIN"
*/

@Service
public class CustomUserDetailsService implements ReactiveUserDetailsService {

    private final UserRepo userRepository;

    public CustomUserDetailsService(UserRepo userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public Mono<UserDetails> findByUsername(String username) {
        return userRepository.findUserByUsername(username)
                .switchIfEmpty(Mono.error(new UsernameNotFoundException("User not found: " + username)))
                .map(user -> {
                    List<SimpleGrantedAuthority> authorities = List.of(
                            new SimpleGrantedAuthority(user.getRole())
                    );

                    return new CustomUserDetails(
                            user.getId(), // Pass the ID from your DB entity
                            user.getUsername(),
                            authorities
                    );
                });
    }
}