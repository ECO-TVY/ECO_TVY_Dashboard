package com.ecotvy.application.service;

import com.ecotvy.application.dto.AdminUserDto;
import com.ecotvy.application.dto.AdminUserPageDto;
import com.ecotvy.application.dto.CreateAdminRequest;
import com.ecotvy.application.port.in.AdminUserUseCase;
import com.ecotvy.application.port.out.UserRepositoryPort;
import com.ecotvy.domain.user.AuthProvider;
import com.ecotvy.domain.user.User;
import com.ecotvy.domain.user.UserRole;
import com.ecotvy.infrastructure.error.enums.ErrorCode;
import com.ecotvy.infrastructure.exception.BusinessException;
import com.ecotvy.infrastructure.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AdminUserService implements AdminUserUseCase {

    private static final int MAX_PAGE_SIZE = 100;

    private final UserRepositoryPort userRepositoryPort;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public AdminUserPageDto listUsers(String query, int page, int size) {
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        // 한 건 더 가져와서 다음 페이지 여부를 판단 (전체 count 쿼리 없이)
        List<User> rows = userRepositoryPort.search(query, Math.max(page, 0), safeSize + 1);
        boolean hasMore = rows.size() > safeSize;
        List<AdminUserDto> users = rows.stream().limit(safeSize).map(AdminUserService::toDto).toList();
        return new AdminUserPageDto(users, hasMore);
    }

    @Override
    public AdminUserDto createAdmin(CreateAdminRequest request) {
        String email = AccountRules.email(request.getEmail());
        String password = AccountRules.password(request.getPassword());
        String nickname = AccountRules.nickname(request.getNickname());
        if (userRepositoryPort.existsByEmail(email)) {
            // 이미 가입한 사람이면 새로 만들지 말고 권한 변경을 쓰도록 안내
            throw new BusinessException(ErrorCode.USER_ALREADY_EXISTS,
                    "이미 가입된 이메일입니다. 사용자 목록에서 관리자로 지정하세요");
        }

        User saved = userRepositoryPort.save(User.builder()
                .email(email)
                .password(passwordEncoder.encode(password))
                .nickname(nickname)
                .authProvider(AuthProvider.EMAIL)
                .role(UserRole.ADMIN)
                .mustChangePassword(true) // 관리자가 정한 초기 비밀번호는 첫 로그인 후 반드시 변경
                .build());
        log.info("Admin account created: id={}, email={}", saved.getId(), saved.getEmail());
        return toDto(saved);
    }

    @Override
    public AdminUserDto changeRole(Long actorId, Long targetUserId, String role) {
        UserRole newRole;
        try {
            newRole = UserRole.valueOf(role);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "권한은 USER 또는 ADMIN이어야 합니다");
        }
        if (targetUserId.equals(actorId)) {
            throw new BusinessException(ErrorCode.OPERATION_NOT_ALLOWED,
                    "자기 자신의 권한은 바꿀 수 없습니다. 다른 관리자에게 요청하세요");
        }

        User user = userRepositoryPort.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        UserRole before = user.getRole();
        user.changeRole(newRole);
        User saved = userRepositoryPort.save(user);
        if (before != newRole) {
            log.info("User role changed: target={} {} -> {} by admin={}", targetUserId, before, newRole, actorId);
        }
        return toDto(saved);
    }

    private static AdminUserDto toDto(User u) {
        return AdminUserDto.builder()
                .id(u.getId())
                .email(u.getEmail())
                .nickname(u.getNickname())
                .role(u.getRole().name())
                .active(u.getActive())
                .address(u.getAddress())
                .totalClassifications(u.getTotalClassifications())
                .createdAt(u.getCreateDt())
                .build();
    }
}
