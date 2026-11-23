package com.ecotvy.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** 관리자 화면의 사용자 목록 항목 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserDto {
    private Long id;
    private String email;
    private String nickname;
    private String role;
    private Boolean active;
    private String address;
    private Integer totalClassifications;
    private LocalDateTime createdAt;
}
