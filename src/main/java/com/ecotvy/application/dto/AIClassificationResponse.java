package com.ecotvy.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AIClassificationResponse {
    private String wasteType;
    private Double confidence;
    private List<String> labels;
    private String modelVersion;
}

