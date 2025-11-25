package com.ecotvy.application.port.out;

import com.ecotvy.domain.user.User;

import java.util.Optional;

public interface UserRepositoryPort {
    User save(User user);
    Optional<User> findById(Long id);
    Optional<User> findByEmail(String email);
    Optional<User> findByProviderIdAndAuthProvider(String providerId, String authProvider);
    boolean existsByEmail(String email);
}

