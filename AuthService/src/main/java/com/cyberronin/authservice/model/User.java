package com.cyberronin.authservice.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Table("users")
public class User {
    @Id
    private Long id;

    private String name;
    private String username;
    private String password; // stores password hash in db but raw is transmitted from user during registration
    private String role; // USER or ADMIN
}