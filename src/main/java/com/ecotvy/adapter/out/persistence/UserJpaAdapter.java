package com.ecotvy.adapter.out.persistence;

import com.ecotvy.application.port.out.UserRepositoryPort;
import com.ecotvy.domain.user.AuthProvider;
import com.ecotvy.domain.user.User;
import com.ecotvy.adapter.out.persistence.repository.UserJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class UserJpaAdapter implements UserRepositoryPort {

    private final UserJpaRepository userJpaRepository;

    @Override
    public User save(User user) {
        return userJpaRepository.save(user);
    }

    @Override
    public Optional<User> findById(Long id) {
        return userJpaRepository.findById(id);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return userJpaRepository.findByEmail(email);
    }

    @Override
    public Optional<User> findByProviderIdAndAuthProvider(String providerId, String authProvider) {
        return userJpaRepository.findByProviderIdAndAuthProvider(providerId, AuthProvider.valueOf(authProvider));
    }

    @Override
    public boolean existsByEmail(String email) {
        return userJpaRepository.existsByEmail(email);
    }
}

