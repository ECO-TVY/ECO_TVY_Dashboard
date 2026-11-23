package com.ecotvy.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrainingSampleDto {
    private Long id;
    private Long classificationId;
    private Long reportId;
    private String label;
    private String predictedLabel;
    private String modelVersion;
    private String datasetKey;
    private String datasetUri;
    private String imageUrl;
    private LocalDateTime createdAt;
}
