package com.ecotvy.application.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 관리자가 새 관리자 계정을 만들 때의 요청 (주소는 받지 않음) */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CreateAdminRequest {
    private String email;
    private String password;
    private String nickname;
}
