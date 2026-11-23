package com.ecotvy.application.service;

import com.ecotvy.application.dto.UserDto;
import com.ecotvy.application.dto.UserProfileUpdateRequest;
import com.ecotvy.application.port.in.UserManagementUseCase;
import com.ecotvy.application.port.out.UserRepositoryPort;
import com.ecotvy.infrastructure.error.enums.ErrorCode;
import com.ecotvy.infrastructure.exception.AuthenticationException;
import com.ecotvy.infrastructure.exception.BusinessException;
import com.ecotvy.infrastructure.exception.ResourceNotFoundException;
import com.ecotvy.domain.user.AuthProvider;
import com.ecotvy.domain.user.User;
import com.ecotvy.domain.user.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
@Transactional
public class UserManagementService implements UserManagementUseCase {

    private final UserRepositoryPort userRepositoryPort;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserDto registerUser(UserDto userDto) {
        validateRegistration(userDto);
        if (userRepositoryPort.existsByEmail(userDto.getEmail())) {
            throw new BusinessException(ErrorCode.USER_ALREADY_EXISTS);
        }

        User user = User.builder()
                .email(userDto.getEmail())
                .password(passwordEncoder.encode(userDto.getPassword()))
                .nickname(userDto.getNickname())
                .address(userDto.getAddress())
                .detailAddress(userDto.getDetailAddress())
                .latitude(userDto.getLatitude())
                .longitude(userDto.getLongitude())
                .authProvider(AuthProvider.EMAIL)
                .role(UserRole.USER)
                .build();

        User saved = userRepositoryPort.save(user);
        return toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public UserDto login(String email, String password) {
        if (email == null || password == null) {
            throw new AuthenticationException(ErrorCode.INVALID_CREDENTIALS);
        }

        // 이메일 존재 여부가 드러나지 않도록 "없는 계정"과 "비밀번호 불일치"를 같은 에러로 응답
        User user = userRepositoryPort.findByEmail(email)
                .orElseThrow(() -> new AuthenticationException(ErrorCode.INVALID_CREDENTIALS));

        if (user.getPassword() == null || !passwordEncoder.matches(password, user.getPassword())) {
            throw new AuthenticationException(ErrorCode.INVALID_CREDENTIALS);
        }
        if (!Boolean.TRUE.equals(user.getActive())) {
            throw new AuthenticationException(ErrorCode.FORBIDDEN, "비활성화된 계정입니다");
        }

        return toDto(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserDto getUserById(Long userId) {
        User user = userRepositoryPort.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return toDto(user);
    }

    @Override
    public UserDto updateProfile(Long userId, UserProfileUpdateRequest request) {
        User user = userRepositoryPort.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.updateProfile(
                request.getNickname(),
                request.getAddress(),
                request.getDetailAddress()
        );

        User saved = userRepositoryPort.save(user);
        return toDto(saved);
    }

    @Override
    public UserDto changePassword(Long userId, String currentPassword, String newPassword) {
        User user = userRepositoryPort.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (user.getPassword() == null) {
            // 소셜 로그인 등 비밀번호가 없는 계정
            throw new BusinessException(ErrorCode.OPERATION_NOT_ALLOWED, "비밀번호로 로그인하는 계정이 아닙니다");
        }
        // 401이 아닌 400: 프론트엔드가 401을 받으면 로그아웃시키므로, 입력 오류로 응답
        if (currentPassword == null || !passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new BusinessException(ErrorCode.CURRENT_PASSWORD_MISMATCH);
        }
        AccountRules.password(newPassword);
        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new BusinessException(ErrorCode.SAME_AS_CURRENT_PASSWORD);
        }
        user.changePassword(passwordEncoder.encode(newPassword));
        return toDto(userRepositoryPort.save(user));
    }

    @Override
    public void updateLocation(Long userId, Double latitude, Double longitude) {
        User user = userRepositoryPort.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.updateLocation(latitude, longitude);
        userRepositoryPort.save(user);
    }

    private void validateRegistration(UserDto dto) {
        AccountRules.email(dto.getEmail());
        AccountRules.password(dto.getPassword());
        AccountRules.nickname(dto.getNickname());
        // 일반 회원은 동네 배출 규정·근처 배출 장소 안내를 위해 주소·좌표 필수 (관리자 계정은 선택)
        if (isBlank(dto.getAddress())
                || dto.getLatitude() == null || dto.getLongitude() == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private UserDto toDto(User user) {
        return UserDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .role(user.getRole())
                .mustChangePassword(user.requiresPasswordChange())
                .nickname(user.getNickname())
                .address(user.getAddress())
                .detailAddress(user.getDetailAddress())
                .latitude(user.getLatitude())
                .longitude(user.getLongitude())
                .level(user.getLevel())
                .totalClassifications(user.getTotalClassifications())
                .build();
    }
}

