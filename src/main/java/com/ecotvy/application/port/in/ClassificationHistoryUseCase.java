package com.ecotvy.application.port.in;

import com.ecotvy.application.dto.ClassificationHistoryDto;

import java.util.List;

public interface ClassificationHistoryUseCase {
    List<ClassificationHistoryDto> getHistory(Long userId, Integer page, Integer size);
    ClassificationHistoryDto getHistoryById(Long historyId, Long userId);
    void reportMisclassification(Long classificationId, Long userId, String correctType);
}

