package com.ecotvy.domain.user;

import com.ecotvy.infrastructure.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    private String password;

    @Column(nullable = false)
    private String nickname;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuthProvider authProvider;

    private String providerId;

    // 주소·좌표는 선택: 일반 회원가입은 서비스에서 필수로 검증하지만, 관리자가 만든 관리자 계정은 없을 수 있다
    private String address; // 시/구/동

    private String detailAddress;

    private Double latitude;

    private Double longitude;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private UserRole role = UserRole.USER;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Builder.Default
    private Integer level = 1; // 배출 성취도 레벨

    @Builder.Default
    private Integer totalClassifications = 0; // 총 분류 횟수

    /**
     * 다음 로그인 후 비밀번호를 반드시 바꿔야 하는지 (관리자가 만든 계정의 초기 비밀번호).
     * 기존 행은 컬럼 추가 시 NULL이므로 requiresPasswordChange()로 판단한다.
     */
    @Builder.Default
    private Boolean mustChangePassword = false;

    private LocalDateTime passwordChangedAt;

    public void updateProfile(String nickname, String address, String detailAddress) {
        if (nickname != null) this.nickname = nickname;
        if (address != null) this.address = address;
        if (detailAddress != null) this.detailAddress = detailAddress;
    }

    public void updateLocation(Double latitude, Double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public void changePassword(String encodedPassword) {
        this.password = encodedPassword;
        this.mustChangePassword = false;
        this.passwordChangedAt = LocalDateTime.now();
    }

    /** 관리자 초기화 등으로 받은 임시 비밀번호 → 다음 로그인 때 변경 강제 */
    public void requirePasswordChange() {
        this.mustChangePassword = true;
    }

    public boolean requiresPasswordChange() {
        return Boolean.TRUE.equals(mustChangePassword);
    }

    public void changeRole(UserRole role) {
        this.role = role;
    }

    public void incrementClassification() {
        this.totalClassifications = (this.totalClassifications == null ? 0 : this.totalClassifications) + 1;
        updateLevel();
    }

    private void updateLevel() {
        // 레벨 계산 로직 (예: 10회마다 레벨 증가)
        if (this.totalClassifications != null) {
            this.level = (this.totalClassifications / 10) + 1;
        }
    }
}

