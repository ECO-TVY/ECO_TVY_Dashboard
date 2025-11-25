package com.ecotvy.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocationRuleDto {
    private Long id;
    private String city;
    private String district;
    private String dong;
    private String wasteType;
    private String disposalDay;
    private String disposalTime;
    private String disposalGuide;
}

