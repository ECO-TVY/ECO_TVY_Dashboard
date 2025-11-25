package com.ecotvy.domain.waste;

import com.ecotvy.infrastructure.entity.BaseTimeEntity;
import com.ecotvy.domain.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Table(name = "classifications")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Classification extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String imageUrl; // S3 URL

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WasteType wasteType;

    @Column(nullable = false)
    private Double confidence; // AI 신뢰도 (0.0 ~ 1.0)

    @ElementCollection
    @CollectionTable(name = "classification_labels", joinColumns = @JoinColumn(name = "classification_id"))
    @Column(name = "label")
    private List<String> labels; // 멀티 레이블 분류 결과

    @Column(nullable = false)
    private String aiModelVersion; // AI 모델 버전

    private String disposalGuide; // 분리배출 가이드
    private String disposalDay; // 배출 요일
    private String disposalTime; // 배출 시간

    @Builder.Default
    private Boolean isCorrect = null; // 사용자 피드백 (null: 미확인, true: 정확, false: 오분류)

    public void updateFeedback(Boolean isCorrect) {
        this.isCorrect = isCorrect;
    }

    public void setDisposalGuide(String disposalGuide) {
        this.disposalGuide = disposalGuide;
    }

    public void setDisposalDay(String disposalDay) {
        this.disposalDay = disposalDay;
    }

    public void setDisposalTime(String disposalTime) {
        this.disposalTime = disposalTime;
    }
}

