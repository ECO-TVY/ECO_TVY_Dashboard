package com.ecotvy.adapter.in.web;

import com.ecotvy.application.dto.*;
import com.ecotvy.application.port.in.AdminUseCase;
import com.ecotvy.infrastructure.dto.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminUseCase adminUseCase;

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<AdminDashboardDto>> getDashboard() {
        AdminDashboardDto result = adminUseCase.getDashboard();
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/location-rules")
    public ResponseEntity<ApiResponse<List<LocationRuleDto>>> getLocationRules(
            @RequestParam String city,
            @RequestParam String district,
            @RequestParam String dong) {
        List<LocationRuleDto> result = adminUseCase.getLocationRules(city, district, dong);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/location-rules")
    public ResponseEntity<ApiResponse<LocationRuleDto>> createLocationRule(
            @RequestBody LocationRuleDto ruleDto) {
        LocationRuleDto result = adminUseCase.createLocationRule(ruleDto);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PutMapping("/location-rules/{id}")
    public ResponseEntity<ApiResponse<LocationRuleDto>> updateLocationRule(
            @PathVariable Long id,
            @RequestBody LocationRuleDto ruleDto) {
        LocationRuleDto result = adminUseCase.updateLocationRule(id, ruleDto);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @DeleteMapping("/location-rules/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteLocationRule(@PathVariable Long id) {
        adminUseCase.deleteLocationRule(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/misclassifications")
    public ResponseEntity<ApiResponse<List<MisclassificationReviewDto>>> getPendingMisclassifications() {
        List<MisclassificationReviewDto> result = adminUseCase.getPendingMisclassifications();
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/misclassifications/{id}/approve")
    public ResponseEntity<ApiResponse<Void>> approveMisclassification(
            @PathVariable Long id,
            @RequestParam(defaultValue = "true") Boolean addToTrainingData) {
        adminUseCase.approveMisclassification(id, addToTrainingData);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/misclassifications/{id}/reject")
    public ResponseEntity<ApiResponse<Void>> rejectMisclassification(
            @PathVariable Long id,
            @RequestParam String reason) {
        adminUseCase.rejectMisclassification(id, reason);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/ai-models")
    public ResponseEntity<ApiResponse<List<AIModelDto>>> getAIModels() {
        List<AIModelDto> result = adminUseCase.getAIModels();
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/ai-models/{version}/activate")
    public ResponseEntity<ApiResponse<AIModelDto>> activateAIModel(@PathVariable String version) {
        AIModelDto result = adminUseCase.activateAIModel(version);
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}

