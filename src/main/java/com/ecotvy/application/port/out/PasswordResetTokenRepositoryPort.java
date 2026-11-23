package com.ecotvy.application.port.out;

import com.ecotvy.domain.user.PasswordResetToken;

import java.time.LocalDateTime;
import java.util.Optional;

public interface PasswordResetTokenRepositoryPort {
    PasswordResetToken save(PasswordResetToken token);
    Optional<PasswordResetToken> findByTokenHash(String tokenHash);
    /** 해당 사용자의 아직 쓰지 않은 토큰을 모두 무효화 (새 링크 발급, 재설정 완료, 관리자 초기화 시) */
    void invalidateUnusedForUser(Long userId, LocalDateTime now);
}
