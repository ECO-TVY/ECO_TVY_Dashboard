package com.ecotvy.adapter.out.persistence.repository;

import com.ecotvy.domain.waste.Classification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ClassificationJpaRepository extends JpaRepository<Classification, Long> {
    List<Classification> findByUserIdOrderByCreateDtDesc(Long userId, Pageable pageable);
    Long countByUserId(Long userId);
    
    @Query("SELECT COUNT(c) FROM Classification c WHERE DATE(c.createDt) = CURRENT_DATE")
    Long countTodayClassifications();
}

