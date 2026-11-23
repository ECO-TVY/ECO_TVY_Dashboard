package com.ecotvy.adapter.in.web;

import com.ecotvy.application.port.in.PasswordResetUseCase;
import com.ecotvy.infrastructure.dto.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** 비밀번호를 잊은 사용자용 (로그인 없이 호출) */
@RestController
@RequestMapping("/api/users/password-reset")
@RequiredArgsConstructor
public class PasswordResetController {

    private final PasswordResetUseCase passwordResetUseCase;

    /** body: {"email": "..."} — 가입 여부와 관계없이 항상 같은 응답 */
    @PostMapping("/request")
    public ResponseEntity<ApiResponse<Void>> request(@RequestBody Map<String, String> body) {
        passwordResetUseCase.requestReset(body.get("email"));
        return ResponseEntity.ok(ApiResponse.success(null,
                "입력한 이메일로 가입된 계정이 있으면 재설정 링크를 보냈습니다"));
    }

    /** body: {"token": "...", "newPassword": "..."} */
    @PostMapping("/confirm")
    public ResponseEntity<ApiResponse<Void>> confirm(@RequestBody Map<String, String> body) {
        passwordResetUseCase.confirmReset(body.get("token"), body.get("newPassword"));
        return ResponseEntity.ok(ApiResponse.success(null, "비밀번호를 재설정했습니다"));
    }
}
