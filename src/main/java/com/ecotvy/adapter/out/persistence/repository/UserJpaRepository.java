package com.ecotvy.adapter.out.persistence.repository;

import com.ecotvy.domain.user.AuthProvider;
import com.ecotvy.domain.user.User;
import com.ecotvy.domain.user.UserRole;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserJpaRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByProviderIdAndAuthProvider(String providerId, AuthProvider authProvider);
    boolean existsByEmail(String email);

    long countByRole(UserRole role);

    @Query("SELECT u FROM User u WHERE :q IS NULL "
            + "OR LOWER(u.email) LIKE LOWER(CONCAT('%', :q, '%')) "
            + "OR LOWER(u.nickname) LIKE LOWER(CONCAT('%', :q, '%')) "
            + "ORDER BY u.id DESC")
    List<User> search(@Param("q") String query, Pageable pageable);
}

