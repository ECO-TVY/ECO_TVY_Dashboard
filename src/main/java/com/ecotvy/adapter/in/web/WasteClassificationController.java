package com.ecotvy.adapter.in.web;

import com.ecotvy.application.dto.ClassificationResult;
import com.ecotvy.application.port.in.ClassifyWasteUseCase;
import com.ecotvy.infrastructure.dto.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/waste")
@RequiredArgsConstructor
public class WasteClassificationController {

    private final ClassifyWasteUseCase classifyWasteUseCase;

    @PostMapping("/classify")
    public ResponseEntity<ApiResponse<ClassificationResult>> classifyWaste(
            @RequestParam("image") MultipartFile imageFile,
            @AuthenticationPrincipal Long userId) {

        ClassificationResult result = classifyWasteUseCase.classifyWaste(userId, imageFile);
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}

