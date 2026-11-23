package com.ecotvy.application.port.out;

import com.ecotvy.domain.waste.Classification;
import com.ecotvy.domain.waste.WasteType;

import java.util.List;
import java.util.Optional;

public interface ClassificationRepositoryPort {
    Classification save(Classification classification);
    Optional<Classification> findById(Long id);
    List<Classification> findByUserIdOrderByCreateDtDesc(Long userId, Integer page, Integer size);
    List<Classification> findByUserIdAndWasteTypeOrderByCreateDtDesc(Long userId, WasteType wasteType, Integer page, Integer size);
    Long countByUserId(Long userId);
}

