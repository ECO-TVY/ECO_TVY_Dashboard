package com.ecotvy.adapter.in.web;

import com.ecotvy.application.dto.TrainingDataSummaryDto;
import com.ecotvy.application.dto.TrainingSampleDto;
import com.ecotvy.application.port.in.TrainingDataUseCase;
import com.ecotvy.infrastructure.dto.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/admin/training-data")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class TrainingDataController {

    private final TrainingDataUseCase trainingDataUseCase;

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<TrainingDataSummaryDto>> getSummary() {
        return ResponseEntity.ok(ApiResponse.success(trainingDataUseCase.getSummary()));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<TrainingSampleDto>>> getSamples(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(trainingDataUseCase.getSamples(page, size)));
    }

    /** 학습 스크립트용 매니페스트 (CSV 파일 다운로드) */
    @GetMapping(value = "/export", produces = "text/csv")
    public ResponseEntity<byte[]> export() {
        byte[] body = trainingDataUseCase.exportManifestCsv().getBytes(StandardCharsets.UTF_8);
        String filename = "ecotvy-training-manifest-" + LocalDate.now() + ".csv";
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(filename).build().toString())
                .body(body);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> remove(@PathVariable Long id) {
        trainingDataUseCase.removeSample(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
