package com.ecotvy.adapter.in.web;

import com.ecotvy.application.dto.AdminUserDto;
import com.ecotvy.application.dto.AdminUserPageDto;
import com.ecotvy.application.dto.CreateAdminRequest;
import com.ecotvy.application.port.in.AdminUserUseCase;
import com.ecotvy.application.port.in.PasswordResetUseCase;
import com.ecotvy.infrastructure.dto.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final AdminUserUseCase adminUserUseCase;
    private final PasswordResetUseCase passwordResetUseCase;

    /** 사용자 목록 (이메일·닉네임 검색) */
    @GetMapping
    public ResponseEntity<ApiResponse<AdminUserPageDto>> list(
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(adminUserUseCase.listUsers(query, page, size)));
    }

    /** 새 관리자 계정 생성 */
    @PostMapping
    public ResponseEntity<ApiResponse<AdminUserDto>> createAdmin(@RequestBody CreateAdminRequest request) {
        return ResponseEntity.ok(ApiResponse.success(adminUserUseCase.createAdmin(request)));
    }

    /**
     * 비밀번호 초기화: 임시 비밀번호로 바꾸고 다음 로그인 때 변경을 강제.
     * 응답의 임시 비밀번호는 저장되지 않으므로 이 응답에서만 확인할 수 있다.
     */
    @PostMapping("/{id}/password-reset")
    public ResponseEntity<ApiResponse<Map<String, String>>> resetPassword(
            @AuthenticationPrincipal Long actorId,
            @PathVariable Long id) {
        String temp = passwordResetUseCase.resetByAdmin(actorId, id);
        return ResponseEntity.ok()
                .header("Cache-Control", "no-store") // 임시 비밀번호가 캐시에 남지 않게
                .body(ApiResponse.success(Map.of("temporaryPassword", temp)));
    }

    /** 권한 변경 — body: {"role": "ADMIN" | "USER"} */
    @PatchMapping("/{id}/role")
    public ResponseEntity<ApiResponse<AdminUserDto>> changeRole(
            @AuthenticationPrincipal Long actorId,
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(ApiResponse.success(adminUserUseCase.changeRole(actorId, id, body.get("role"))));
    }
}
