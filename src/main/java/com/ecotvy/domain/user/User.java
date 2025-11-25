package com.ecotvy.domain.user;

import com.ecotvy.infrastructure.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

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

    @Column(nullable = false)
    private String address; // 시/구/동

    private String detailAddress;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private UserRole role = UserRole.USER;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    private Integer level; // 배출 성취도 레벨
    private Integer totalClassifications; // 총 분류 횟수

    public void updateProfile(String nickname, String address, String detailAddress) {
        if (nickname != null) this.nickname = nickname;
        if (address != null) this.address = address;
        if (detailAddress != null) this.detailAddress = detailAddress;
    }

    public void updateLocation(Double latitude, Double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
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

