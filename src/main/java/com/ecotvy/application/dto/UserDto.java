package com.ecotvy.application.dto;

import com.ecotvy.domain.user.UserRole;
import com.fasterxml.jackson.annotation.JsonProperty;
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
    // 요청(회원가입)에서만 받고 응답 JSON에는 절대 포함되지 않음
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;
    // 응답 전용: 요청 JSON으로 role을 보내도 무시됨 (가입 시 권한 상승 방지)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private UserRole role;
    // 응답 전용: 초기 비밀번호를 바꿔야 하는 계정이면 true (프론트엔드가 변경 화면으로 보냄)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Boolean mustChangePassword;
    private String nickname;
    private String address;
    private String detailAddress;
    private Double latitude;
    private Double longitude;
    private Integer level;
    private Integer totalClassifications;
}

