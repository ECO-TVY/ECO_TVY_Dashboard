package com.ecotvy.application.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LoginResponse {
    private String accessToken;
    private String tokenType;   // "Bearer"
    private long expiresIn;     // 초 단위
    private UserDto user;
}
