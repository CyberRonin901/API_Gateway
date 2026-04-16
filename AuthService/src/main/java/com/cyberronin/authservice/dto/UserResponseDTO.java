package com.cyberronin.authservice.dto;

import java.time.LocalDateTime;

public record UserResponseDTO(
        String username,
        String name,
        String role,
        LocalDateTime createdAt
) {}