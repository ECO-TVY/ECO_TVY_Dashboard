package com.ecotvy.domain.admin;

import com.ecotvy.domain.waste.Classification;
import com.ecotvy.domain.waste.WasteType;
import com.ecotvy.infrastructure.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * 모델 재학습용 데이터 샘플.
 * 이미지는 저장소의 datasetKey 위치(training/{label}/...)에 복사되어 원본이 지워져도 유지된다.
 */
@Entity
@Table(name = "training_samples",
        uniqueConstraints = @UniqueConstraint(name = "uk_training_sample_classification",
                columnNames = "classification_id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class TrainingSample extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "classification_id", nullable = false)
    private Classification classification;

    /** 이 샘플을 만든 오분류 신고 (신고 외 경로로 추가되면 null) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "report_id")
    private MisclassificationReport report;

    /** 정답 라벨 (학습에 쓰는 라벨) */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WasteType label;

    /** AI가 원래 예측한 라벨 (오분류 패턴 분석용) */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WasteType predictedLabel;

    /** 예측 당시 모델 버전 */
    private String modelVersion;

    /** 저장소 내 데이터셋 위치 (예: training/PLASTIC/123.jpg) */
    @Column(nullable = false, unique = true)
    private String datasetKey;
}
