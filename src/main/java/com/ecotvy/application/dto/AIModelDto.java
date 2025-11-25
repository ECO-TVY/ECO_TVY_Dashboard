package com.ecotvy.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AIModelDto {
    private Long id;
    private String version;
    private String modelUrl;
    private Boolean isActive;
    private String description;
    private Double accuracy;
}

