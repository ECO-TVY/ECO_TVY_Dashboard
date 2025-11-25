package com.ecotvy.adapter.in.web;

import com.ecotvy.application.dto.UserDto;
import com.ecotvy.application.dto.UserProfileUpdateRequest;
import com.ecotvy.application.port.in.UserManagementUseCase;
import com.ecotvy.infrastructure.dto.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserManagementUseCase userManagementUseCase;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserDto>> register(@RequestBody UserDto userDto) {
        UserDto result = userManagementUseCase.registerUser(userDto);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<UserDto>> login(
            @RequestParam String email,
            @RequestParam String password) {
        UserDto result = userManagementUseCase.login(email, password);
        return ResponseEntity.ok(ApiResponse.success(result));
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

