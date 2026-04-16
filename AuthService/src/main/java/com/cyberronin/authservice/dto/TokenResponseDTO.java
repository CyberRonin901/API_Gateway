package com.cyberronin.authservice.dto;

public record TokenResponseDTO(
        String token,
        String type,
        long expiresIn
) {
    public TokenResponseDTO(String token, long expiresIn) {
        this(token, "Bearer", expiresIn);
    }
}