package com.ecotvy.domain.user;

import com.ecotvy.infrastructure.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 비밀번호 재설정 링크 토큰.
 * 토큰 원문은 메일로만 나가고 DB에는 SHA-256 해시만 저장한다 (DB가 유출돼도 링크를 만들 수 없음).
 */
@Entity
@Table(name = "password_reset_tokens",
        indexes = @Index(name = "idx_password_reset_user", columnList = "user_id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class PasswordResetToken extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    /** 사용(또는 새 토큰 발급으로 무효화)된 시각. null이면 아직 유효 */
    private LocalDateTime usedAt;

    public boolean isUsable(LocalDateTime now) {
        return usedAt == null && now.isBefore(expiresAt);
    }

    public void markUsed(LocalDateTime now) {
        this.usedAt = now;
    }
}
