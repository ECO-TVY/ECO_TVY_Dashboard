package com.ecotvy.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {
    private Long id;
    private String email;
    private String nickname;
    private String address;
    private String detailAddress;
    private Double latitude;
    private Double longitude;
    private Integer level;
    private Integer totalClassifications;
}

