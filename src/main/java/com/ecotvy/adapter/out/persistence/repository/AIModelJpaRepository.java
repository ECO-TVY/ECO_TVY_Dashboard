package com.ecotvy.adapter.out.persistence.repository;

import com.ecotvy.domain.admin.AIModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AIModelJpaRepository extends JpaRepository<AIModel, Long> {
    Optional<AIModel> findByIsActiveTrue();
    Optional<AIModel> findByVersion(String version);
}

