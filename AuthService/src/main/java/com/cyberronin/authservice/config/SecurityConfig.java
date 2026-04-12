package com.cyberronin.authservice.config;

import com.cyberronin.authservice.filter.JwtFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity // For using with web flux
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http, JwtFilter jwtFilter) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable) // done at gateway
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable) // done at gateway
                .authorizeExchange(exchanges -> exchanges
//                        .pathMatchers("/auth/admin/register").hasAuthority("ROLE_ADMIN") // Only admin can make another admin
//                        .pathMatchers("/auth/user/register").permitAll() // Open gates for login/register
//                        .pathMatchers("/auth/login").permitAll()
//
//                        .pathMatchers("/admin/**").hasAuthority("ROLE_ADMIN")
//                        .pathMatchers("/user/**").hasAnyAuthority("ROLE_USER", "ROLE_ADMIN")
                        // Any other authenticated request only admin can access.pathMatchers()
//                        .anyExchange().hasAuthority("ROLE_ADMIN")
                                .anyExchange().permitAll()
                )
//                .addFilterAt(jwtFilter, SecurityWebFiltersOrder.AUTHENTICATION) // done at gateway
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}