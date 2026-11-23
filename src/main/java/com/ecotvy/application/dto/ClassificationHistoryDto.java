package com.ecotvy.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClassificationHistoryDto {
    private Long id;
    private String imageUrl;
    private String wasteType;
    private String wasteTypeDisplayName;
    private Double confidence;
    private List<String> labels;
    private String disposalGuide;
    private String disposalDay;
    private String disposalTime;
    // 사용자가 오분류로 신고한 기록이면 true (같은 기록을 두 번 신고하지 않도록 화면에서 표시)
    private Boolean reported;
    private LocalDateTime createdAt;
}

