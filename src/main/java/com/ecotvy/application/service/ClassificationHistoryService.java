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

    static final int MAX_PAGE_SIZE = 100;

    @Override
    public List<ClassificationHistoryDto> getHistory(Long userId, Integer page, Integer size, String wasteType) {
        // 과도한 조회 방지: page는 0 이상, size는 1~100
        int safePage = page == null ? 0 : Math.max(0, page);
        int safeSize = size == null ? 20 : Math.min(MAX_PAGE_SIZE, Math.max(1, size));

        List<Classification> classifications = (wasteType == null || wasteType.isBlank())
                ? classificationRepositoryPort.findByUserIdOrderByCreateDtDesc(userId, safePage, safeSize)
                : classificationRepositoryPort.findByUserIdAndWasteTypeOrderByCreateDtDesc(
                        userId, parseWasteType(wasteType), safePage, safeSize);

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
        // 같은 기록을 여러 번 신고하면 검수 대기열에 중복으로 쌓인다
        if (Boolean.FALSE.equals(classification.getIsCorrect())) {
            throw new BusinessException(ErrorCode.DATA_ALREADY_EXISTS, "이미 신고한 기록입니다");
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

    private static WasteType parseWasteType(String value) {
        try {
            return WasteType.valueOf(value.trim());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "알 수 없는 쓰레기 종류입니다");
        }
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
                .disposalTime(classification.getDisposalTime())
                .reported(Boolean.FALSE.equals(classification.getIsCorrect()))
                .createdAt(classification.getCreateDt())
                .build();
    }
}

