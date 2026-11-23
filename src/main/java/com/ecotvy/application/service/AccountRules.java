package com.ecotvy.application.service;

import com.ecotvy.infrastructure.error.enums.ErrorCode;
import com.ecotvy.infrastructure.exception.BusinessException;

import java.util.regex.Pattern;

/**
 * 계정 입력값 규칙. 회원가입(UserManagementService)과 관리자 계정 생성(AdminUserService)이 같은 규칙을 쓴다.
 * 프론트엔드 RegisterView / 관리자 사용자 탭과 동일한 규칙.
 */
final class AccountRules {

    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    // 8자 이상, 영문과 숫자를 각각 1자 이상 포함
    private static final Pattern PASSWORD = Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d).{8,64}$");
    private static final int NICKNAME_MAX = 20;

    static String email(String email) {
        if (email == null || !EMAIL.matcher(email.trim()).matches()) {
            throw new BusinessException(ErrorCode.INVALID_EMAIL_FORMAT);
        }
        return email.trim();
    }

    static String password(String password) {
        if (password == null || !PASSWORD.matcher(password).matches()) {
            throw new BusinessException(ErrorCode.INVALID_PASSWORD_FORMAT);
        }
        return password;
    }

    static String nickname(String nickname) {
        if (nickname == null || nickname.isBlank() || nickname.trim().length() > NICKNAME_MAX) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "닉네임은 1~" + NICKNAME_MAX + "자로 입력해 주세요");
        }
        return nickname.trim();
    }

    private AccountRules() {
    }
}
