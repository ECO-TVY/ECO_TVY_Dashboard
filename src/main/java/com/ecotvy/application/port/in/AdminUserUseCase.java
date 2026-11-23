package com.ecotvy.application.port.in;

import com.ecotvy.application.dto.AdminUserDto;
import com.ecotvy.application.dto.AdminUserPageDto;
import com.ecotvy.application.dto.CreateAdminRequest;

public interface AdminUserUseCase {

    AdminUserPageDto listUsers(String query, int page, int size);

    /** 새 관리자 계정 생성 (이메일 가입, 주소 없음) */
    AdminUserDto createAdmin(CreateAdminRequest request);

    /**
     * 사용자 권한 변경 (USER ↔ ADMIN).
     * 자기 자신의 권한은 바꿀 수 없다 → API로는 관리자가 0명이 될 수 없다.
     */
    AdminUserDto changeRole(Long actorId, Long targetUserId, String role);
}
