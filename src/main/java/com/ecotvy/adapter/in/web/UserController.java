package com.ecotvy.adapter.in.web;

import com.ecotvy.application.dto.ChangePasswordRequest;
import com.ecotvy.application.dto.LoginRequest;
import com.ecotvy.application.dto.LoginResponse;
import com.ecotvy.application.dto.UserDto;
import com.ecotvy.application.dto.UserProfileUpdateRequest;
import com.ecotvy.application.port.in.UserManagementUseCase;
import com.ecotvy.infrastructure.dto.response.ApiResponse;
import com.ecotvy.infrastructure.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserManagementUseCase userManagementUseCase;
    private final JwtTokenProvider jwtTokenProvider;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserDto>> register(@RequestBody UserDto userDto) {
        UserDto result = userManagementUseCase.registerUser(userDto);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@RequestBody LoginRequest request) {
        UserDto user = userManagementUseCase.login(request.getEmail(), request.getPassword());
        return ResponseEntity.ok(ApiResponse.success(issueToken(user)));
    }

    /**
     * 비밀번호 변경. 새 토큰을 함께 돌려줘서 현재 세션이 이어지게 한다
     * (초기 비밀번호 변경 시에는 '변경 필요' 표시가 없는 토큰으로 교체됨).
     */
    @PutMapping("/me/password")
    public ResponseEntity<ApiResponse<LoginResponse>> changePassword(
            @AuthenticationPrincipal Long userId,
            @RequestBody ChangePasswordRequest request) {
        UserDto user = userManagementUseCase.changePassword(userId, request.getCurrentPassword(), request.getNewPassword());
        return ResponseEntity.ok(ApiResponse.success(issueToken(user)));
    }

    private LoginResponse issueToken(UserDto user) {
        String accessToken = jwtTokenProvider.createAccessToken(
                user.getId(), user.getRole(), Boolean.TRUE.equals(user.getMustChangePassword()));
        long expiresInSeconds = jwtTokenProvider.getAccessTokenExpirationMs() / 1000;
        return new LoginResponse(accessToken, "Bearer", expiresInSeconds, user);
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserDto>> getCurrentUser(
            @AuthenticationPrincipal Long userId) {
        UserDto result = userManagementUseCase.getUserById(userId);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PutMapping("/me/profile")
    public ResponseEntity<ApiResponse<UserDto>> updateProfile(
            @AuthenticationPrincipal Long userId,
            @RequestBody UserProfileUpdateRequest request) {
        UserDto result = userManagementUseCase.updateProfile(userId, request);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PutMapping("/me/location")
    public ResponseEntity<ApiResponse<Void>> updateLocation(
            @AuthenticationPrincipal Long userId,
            @RequestParam Double latitude,
            @RequestParam Double longitude) {
        userManagementUseCase.updateLocation(userId, latitude, longitude);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}

