package com.ecotvy.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MisclassificationReviewDto {
    private Long id;
    private Long classificationId;
    private String imageUrl;
    private String aiClassifiedType;
    private String correctType;
    private Double confidence;
}

