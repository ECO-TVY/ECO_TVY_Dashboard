package com.ecotvy.adapter.out.persistence.repository;

import com.ecotvy.domain.location.DisposalPoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DisposalPointJpaRepository extends JpaRepository<DisposalPoint, Long> {
    List<DisposalPoint> findByActiveTrue();
}

