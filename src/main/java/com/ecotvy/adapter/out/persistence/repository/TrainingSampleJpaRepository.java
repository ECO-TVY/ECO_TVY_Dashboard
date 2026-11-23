package com.ecotvy.adapter.out.persistence.repository;

import com.ecotvy.domain.admin.TrainingSample;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface TrainingSampleJpaRepository extends JpaRepository<TrainingSample, Long> {

    boolean existsByClassificationId(Long classificationId);

    @Query("SELECT s.label, COUNT(s) FROM TrainingSample s GROUP BY s.label")
    List<Object[]> countGroupByLabel();

    // 목록/내보내기에서 classification·report를 한 번에 가져와 N+1 방지
    @EntityGraph(attributePaths = {"classification", "report"})
    List<TrainingSample> findAllByOrderByIdDesc(Pageable pageable);

    @EntityGraph(attributePaths = {"classification", "report"})
    List<TrainingSample> findAllByOrderByIdAsc();
}
