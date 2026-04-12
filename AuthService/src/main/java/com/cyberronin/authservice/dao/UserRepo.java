package com.cyberronin.authservice.dao;

import com.cyberronin.authservice.model.User;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Mono;

import java.util.Optional;

public interface UserRepo extends R2dbcRepository<User, Long>
{
    Mono<User> findUserByUsername(String username);
}
