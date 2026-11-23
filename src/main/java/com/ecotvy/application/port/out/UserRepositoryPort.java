package com.ecotvy.application.port.out;

import com.ecotvy.domain.user.User;
import com.ecotvy.domain.user.UserRole;

import java.util.List;
import java.util.Optional;

public interface UserRepositoryPort {
    User save(User user);
    Optional<User> findById(Long id);
    Optional<User> findByEmail(String email);
    Optional<User> findByProviderIdAndAuthProvider(String providerId, String authProvider);
    boolean existsByEmail(String email);
    long countByRole(UserRole role);
    /** 이메일·닉네임 부분 일치 검색 (query가 비면 전체), 최신 가입순 */
    List<User> search(String query, int page, int size);
}

