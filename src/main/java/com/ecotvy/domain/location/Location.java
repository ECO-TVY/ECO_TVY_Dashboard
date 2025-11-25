package com.ecotvy.domain.location;

import com.ecotvy.infrastructure.entity.BaseTimeEntity;
import com.ecotvy.domain.waste.WasteType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "locations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Location extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String city; // 시

    @Column(nullable = false)
    private String district; // 구

    @Column(nullable = false)
    private String dong; // 동

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WasteType wasteType;

    @Column(nullable = false)
    private String disposalDay; // 배출 요일 (예: "목요일", "화요일", "매일")

    private String disposalTime; // 배출 시간 (예: "18:00 ~ 24:00")

    @Column(columnDefinition = "TEXT")
    private String disposalGuide; // 세부 가이드라인

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;
}

