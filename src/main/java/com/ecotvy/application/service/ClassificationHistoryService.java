package com.ecotvy.application.service;

import com.ecotvy.application.dto.ClassificationHistoryDto;
import com.ecotvy.application.port.in.ClassificationHistoryUseCase;
import com.ecotvy.application.port.out.ClassificationRepositoryPort;
import com.ecotvy.infrastructure.exception.ResourceNotFoundException;
import com.ecotvy.domain.waste.Classification;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClassificationHistoryService implements ClassificationHistoryUseCase {

    private final ClassificationRepositoryPort classificationRepositoryPort;

    @Override
    public List<ClassificationHistoryDto> getHistory(Long userId, Integer page, Integer size) {
        List<Classification> classifications = classificationRepositoryPort.findByUserIdOrderByCreateDtDesc(
                userId, page, size);

        return classifications.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public ClassificationHistoryDto getHistoryById(Long historyId, Long userId) {
        Classification classification = classificationRepositoryPort.findById(historyId)
                .orElseThrow(() -> new ResourceNotFoundException("Classification not found"));

        if (!classification.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Classification not found");
        }

        return toDto(classification);
    }

    @Override
    @Transactional
    public void reportMisclassification(Long classificationId, Long userId, String correctType) {
        Classification classification = classificationRepositoryPort.findById(classificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Classification not found"));

        if (!classification.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Classification not found");
        }

        classification.updateFeedback(false);
        classificationRepositoryPort.save(classification);
        // TODO: MisclassificationReport 생성 로직 추가
    }

    private ClassificationHistoryDto toDto(Classification classification) {
        return ClassificationHistoryDto.builder()
                .id(classification.getId())
                .imageUrl(classification.getImageUrl())
                .wasteType(classification.getWasteType().name())
                .wasteTypeDisplayName(classification.getWasteType().getDisplayName())
                .confidence(classification.getConfidence())
                .labels(classification.getLabels())
                .disposalGuide(classification.getDisposalGuide())
                .disposalDay(classification.getDisposalDay())
                .createdAt(classification.getCreateDt())
                .build();
    }
}

