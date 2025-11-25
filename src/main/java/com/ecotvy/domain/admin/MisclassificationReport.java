package com.ecotvy.domain.admin;

import com.ecotvy.infrastructure.entity.BaseTimeEntity;
import com.ecotvy.domain.waste.Classification;
import com.ecotvy.domain.waste.WasteType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "misclassification_reports")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class MisclassificationReport extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "classification_id", nullable = false)
    private Classification classification;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WasteType correctType; // 올바른 분류

    @Enumerated(EnumType.STRING)
    private WasteType reportedType; // 신고된 분류

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ReportStatus status = ReportStatus.PENDING; // 검수 상태

    private String adminNote; // 관리자 메모

    public void approve() {
        this.status = ReportStatus.APPROVED;
    }

    public void reject() {
        this.status = ReportStatus.REJECTED;
    }
}

