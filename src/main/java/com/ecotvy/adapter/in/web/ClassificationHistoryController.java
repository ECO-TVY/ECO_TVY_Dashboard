package com.ecotvy.adapter.in.web;

import com.ecotvy.application.dto.ClassificationHistoryDto;
import com.ecotvy.application.port.in.ClassificationHistoryUseCase;
import com.ecotvy.infrastructure.dto.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/history")
@RequiredArgsConstructor
public class ClassificationHistoryController {

    private final ClassificationHistoryUseCase classificationHistoryUseCase;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ClassificationHistoryDto>>> getHistory(
            @AuthenticationPrincipal Long userId,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        List<ClassificationHistoryDto> result = classificationHistoryUseCase.getHistory(userId, page, size);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ClassificationHistoryDto>> getHistoryById(
            @PathVariable Long id,
            @AuthenticationPrincipal Long userId) {
        ClassificationHistoryDto result = classificationHistoryUseCase.getHistoryById(id, userId);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/{id}/report")
    public ResponseEntity<ApiResponse<Void>> reportMisclassification(
            @PathVariable Long id,
            @AuthenticationPrincipal Long userId,
            @RequestParam String correctType) {
        classificationHistoryUseCase.reportMisclassification(id, userId, correctType);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}

