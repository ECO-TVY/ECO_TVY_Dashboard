package com.ecotvy.adapter.out.persistence;

import com.ecotvy.adapter.out.persistence.repository.PasswordResetTokenJpaRepository;
import com.ecotvy.application.port.out.PasswordResetTokenRepositoryPort;
import com.ecotvy.domain.user.PasswordResetToken;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class PasswordResetTokenJpaAdapter implements PasswordResetTokenRepositoryPort {

    private final PasswordResetTokenJpaRepository repository;

    @Override
    public PasswordResetToken save(PasswordResetToken token) {
        return repository.save(token);
    }

    @Override
    public Optional<PasswordResetToken> findByTokenHash(String tokenHash) {
        return repository.findByTokenHash(tokenHash);
    }

    @Override
    public void invalidateUnusedForUser(Long userId, LocalDateTime now) {
        repository.invalidateUnusedForUser(userId, now);
    }
}
