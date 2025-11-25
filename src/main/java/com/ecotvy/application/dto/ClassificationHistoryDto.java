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
    private LocalDateTime createdAt;
}

