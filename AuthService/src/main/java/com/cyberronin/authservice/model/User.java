package com.cyberronin.authservice.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Data
@Builder // Added for easier object creation in Service
@NoArgsConstructor
@AllArgsConstructor
@Table("users")
public class User {
    @Id
    private Long id;
    private String name;
    private String username;
    private String password; // Strictly the BCRYPT HASH
    private String role;
}