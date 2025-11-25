package com.ecotvy.application.port.out;

import com.ecotvy.domain.waste.Classification;

import java.util.List;
import java.util.Optional;

public interface ClassificationRepositoryPort {
    Classification save(Classification classification);
    Optional<Classification> findById(Long id);
    List<Classification> findByUserIdOrderByCreateDtDesc(Long userId, Integer page, Integer size);
    Long countByUserId(Long userId);
}

