package com.ecotvy.domain.admin;

import com.ecotvy.infrastructure.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "ai_models")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class AIModel extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String version; // 모델 버전 (예: "v1.0.3")

    @Column(nullable = false)
    private String modelUrl; // AI 모델 서버 URL

    @Column(nullable = false)
    @Builder.Default
    private Boolean isActive = false; // 현재 사용 중인 모델

    @Column(columnDefinition = "TEXT")
    private String description; // 모델 설명

    private Double accuracy; // 모델 정확도

    public void activate() {
        this.isActive = true;
    }

    public void deactivate() {
        this.isActive = false;
    }
}

