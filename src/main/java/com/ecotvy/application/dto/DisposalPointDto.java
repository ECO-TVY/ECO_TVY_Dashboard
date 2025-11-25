package com.ecotvy.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DisposalPointDto {
    private Long id;
    private String name;
    private Double latitude;
    private Double longitude;
    private String address;
    private String description;
    private Double distanceKm;
}

