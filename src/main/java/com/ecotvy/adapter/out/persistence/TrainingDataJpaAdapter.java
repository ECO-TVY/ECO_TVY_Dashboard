package com.ecotvy.adapter.out.persistence;

import com.ecotvy.adapter.out.persistence.repository.TrainingSampleJpaRepository;
import com.ecotvy.application.port.out.TrainingDataRepositoryPort;
import com.ecotvy.domain.admin.TrainingSample;
import com.ecotvy.domain.waste.WasteType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class TrainingDataJpaAdapter implements TrainingDataRepositoryPort {

    private final TrainingSampleJpaRepository repository;

    @Override
    public TrainingSample save(TrainingSample sample) {
        return repository.save(sample);
    }

    @Override
    public boolean existsByClassificationId(Long classificationId) {
        return repository.existsByClassificationId(classificationId);
    }

    @Override
    public Map<WasteType, Long> countByLabel() {
        Map<WasteType, Long> counts = new EnumMap<>(WasteType.class);
        for (Object[] row : repository.countGroupByLabel()) {
            counts.put((WasteType) row[0], (Long) row[1]);
        }
        return counts;
    }

    @Override
    public List<TrainingSample> findRecent(int page, int size) {
        return repository.findAllByOrderByIdDesc(PageRequest.of(page, size));
    }

    @Override
    public List<TrainingSample> findAllForExport() {
        return repository.findAllByOrderByIdAsc();
    }

    @Override
    public Optional<TrainingSample> findById(Long id) {
        return repository.findById(id);
    }

    @Override
    public void delete(TrainingSample sample) {
        repository.delete(sample);
    }
}
