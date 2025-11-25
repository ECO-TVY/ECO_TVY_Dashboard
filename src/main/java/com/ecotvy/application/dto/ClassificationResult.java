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
public class ClassificationResult {
    private Long classificationId;
    private String wasteType;
    private String wasteTypeDisplayName;
    private Double confidence;
    private List<String> labels;
    private String imageUrl;
    private String disposalGuide;
    private String disposalDay;
    private String disposalTime;
    private List<DisposalPointDto> nearbyDisposalPoints;
}

