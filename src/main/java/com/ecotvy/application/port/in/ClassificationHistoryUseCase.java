package com.ecotvy.application.port.in;

import com.ecotvy.application.dto.ClassificationHistoryDto;

import java.util.List;

public interface ClassificationHistoryUseCase {
    /** @param wasteType 종류 필터 (WasteType 이름, null이면 전체) */
    List<ClassificationHistoryDto> getHistory(Long userId, Integer page, Integer size, String wasteType);
    ClassificationHistoryDto getHistoryById(Long historyId, Long userId);
    void reportMisclassification(Long classificationId, Long userId, String correctType);
}

