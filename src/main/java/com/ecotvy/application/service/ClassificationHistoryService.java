package com.ecotvy.application.service;

import com.ecotvy.application.dto.ClassificationHistoryDto;
import com.ecotvy.application.port.in.ClassificationHistoryUseCase;
import com.ecotvy.application.port.out.AdminRepositoryPort;
import com.ecotvy.application.port.out.ClassificationRepositoryPort;
import com.ecotvy.application.port.out.ImageStoragePort;
import com.ecotvy.domain.admin.MisclassificationReport;
import com.ecotvy.infrastructure.error.enums.ErrorCode;
import com.ecotvy.infrastructure.exception.BusinessException;
import com.ecotvy.infrastructure.exception.ResourceNotFoundException;
import com.ecotvy.domain.waste.Classification;
import com.ecotvy.domain.waste.WasteType;
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
    private final AdminRepositoryPort adminRepositoryPort;
    private final ImageStoragePort imageStoragePort;

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

        WasteType correct;
        try {
            correct = WasteType.valueOf(correctType);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        if (correct == classification.getWasteType()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "AI 분류 결과와 같은 종류로는 신고할 수 없습니다");
        }

        classification.updateFeedback(false);
        classificationRepositoryPort.save(classification);

        // 관리자 검수 대기열에 등록
        adminRepositoryPort.save(MisclassificationReport.builder()
                .classification(classification)
                .reportedType(classification.getWasteType())
                .correctType(correct)
                .build());
    }

    private ClassificationHistoryDto toDto(Classification classification) {
        return ClassificationHistoryDto.builder()
                .id(classification.getId())
                .imageUrl(imageStoragePort.presignedUrl(classification.getImageUrl()))
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

