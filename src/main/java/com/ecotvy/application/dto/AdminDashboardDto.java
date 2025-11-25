package com.ecotvy.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminDashboardDto {
    private Long totalUsers;
    private Long todayClassifications;
    private String activeModelVersion;
    private Long pendingMisclassifications;
}

