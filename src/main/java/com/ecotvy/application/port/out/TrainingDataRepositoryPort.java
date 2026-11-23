package com.ecotvy.application.port.out;

import com.ecotvy.domain.admin.TrainingSample;
import com.ecotvy.domain.waste.WasteType;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface TrainingDataRepositoryPort {
    TrainingSample save(TrainingSample sample);
    boolean existsByClassificationId(Long classificationId);
    Map<WasteType, Long> countByLabel();
    List<TrainingSample> findRecent(int page, int size);
    List<TrainingSample> findAllForExport();
    Optional<TrainingSample> findById(Long id);
    void delete(TrainingSample sample);
}
