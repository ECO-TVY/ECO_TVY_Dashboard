package com.ecotvy.adapter.out.persistence.repository;

import com.ecotvy.domain.user.AuthProvider;
import com.ecotvy.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserJpaRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByProviderIdAndAuthProvider(String providerId, AuthProvider authProvider);
    boolean existsByEmail(String email);
}

