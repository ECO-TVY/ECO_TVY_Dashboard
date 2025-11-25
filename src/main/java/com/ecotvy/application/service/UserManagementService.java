package com.ecotvy.application.service;

import com.ecotvy.application.dto.UserDto;
import com.ecotvy.application.dto.UserProfileUpdateRequest;
import com.ecotvy.application.port.in.UserManagementUseCase;
import com.ecotvy.application.port.out.UserRepositoryPort;
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
        if (userRepositoryPort.existsByEmail(userDto.getEmail())) {
            throw new BusinessException("User already exists");
        }

        User user = User.builder()
                .email(userDto.getEmail())
                .password(passwordEncoder.encode("defaultPassword")) // TODO: 실제 비밀번호 처리 필요
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
        User user = userRepositoryPort.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new BusinessException("Invalid credentials");
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
    public void updateLocation(Long userId, Double latitude, Double longitude) {
        User user = userRepositoryPort.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.updateLocation(latitude, longitude);
        userRepositoryPort.save(user);
    }

    private UserDto toDto(User user) {
        return UserDto.builder()
                .id(user.getId())
                .email(user.getEmail())
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

