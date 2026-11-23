package com.ecotvy.application.service;

import com.ecotvy.application.dto.TrainingDataSummaryDto;
import com.ecotvy.application.dto.TrainingSampleDto;
import com.ecotvy.application.port.in.TrainingDataUseCase;
import com.ecotvy.application.port.out.ImageStoragePort;
import com.ecotvy.application.port.out.TrainingDataRepositoryPort;
import com.ecotvy.domain.admin.MisclassificationReport;
import com.ecotvy.domain.admin.TrainingSample;
import com.ecotvy.domain.waste.Classification;
import com.ecotvy.domain.waste.WasteType;
import com.ecotvy.infrastructure.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TrainingDataService implements TrainingDataUseCase {

    /** 데이터셋 루트. 그 아래 라벨별 폴더(training/PLASTIC/...) → torchvision ImageFolder 등에서 바로 사용 가능 */
    static final String DATASET_PREFIX = "training/";

    private final TrainingDataRepositoryPort trainingDataRepositoryPort;
    private final ImageStoragePort imageStoragePort;

    @Override
    @Transactional
    public void addFromReport(MisclassificationReport report) {
        Classification classification = report.getClassification();
        if (trainingDataRepositoryPort.existsByClassificationId(classification.getId())) {
            log.info("Classification {} is already in the training set; skipping", classification.getId());
            return;
        }

        WasteType label = report.getCorrectType();
        String datasetKey = DATASET_PREFIX + label.name() + "/" + classification.getId()
                + imageStoragePort.extensionOf(classification.getImageUrl());

        // DB 저장을 먼저 해 중복(유니크 제약) 등의 문제를 복사 전에 걸러내고,
        // 복사가 실패하면 예외 → 트랜잭션 롤백으로 기록·승인 모두 취소된다.
        // (반대로 커밋이 실패하면 복사본이 남지만, 키가 분류 ID 기반이라 재시도 시 덮어써진다)
        trainingDataRepositoryPort.save(TrainingSample.builder()
                .classification(classification)
                .report(report)
                .label(label)
                .predictedLabel(classification.getWasteType())
                .modelVersion(classification.getAiModelVersion())
                .datasetKey(datasetKey)
                .build());
        imageStoragePort.copyToDataset(classification.getImageUrl(), datasetKey);
    }

    @Override
    public TrainingDataSummaryDto getSummary() {
        Map<WasteType, Long> counts = trainingDataRepositoryPort.countByLabel();
        Map<String, Long> all = new LinkedHashMap<>();
        for (WasteType type : WasteType.values()) {
            all.put(type.name(), counts.getOrDefault(type, 0L));
        }
        return TrainingDataSummaryDto.builder()
                .total(all.values().stream().mapToLong(Long::longValue).sum())
                .countsByLabel(all)
                .datasetRoot(imageStoragePort.datasetUri(DATASET_PREFIX))
                .build();
    }

    @Override
    public List<TrainingSampleDto> getSamples(int page, int size) {
        int safeSize = Math.min(Math.max(size, 1), 100);
        return trainingDataRepositoryPort.findRecent(Math.max(page, 0), safeSize).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public String exportManifestCsv() {
        StringBuilder sb = new StringBuilder(
                "id,label,predicted_label,model_version,dataset_uri,classification_id,report_id,created_at\n");
        for (TrainingSample s : trainingDataRepositoryPort.findAllForExport()) {
            sb.append(s.getId()).append(',')
                    .append(s.getLabel().name()).append(',')
                    .append(s.getPredictedLabel().name()).append(',')
                    .append(csv(s.getModelVersion())).append(',')
                    .append(csv(imageStoragePort.datasetUri(s.getDatasetKey()))).append(',')
                    .append(s.getClassification().getId()).append(',')
                    .append(s.getReport() == null ? "" : s.getReport().getId()).append(',')
                    .append(s.getCreateDt() == null ? "" : s.getCreateDt()).append('\n');
        }
        return sb.toString();
    }

    @Override
    @Transactional
    public void removeSample(Long sampleId) {
        TrainingSample sample = trainingDataRepositoryPort.findById(sampleId)
                .orElseThrow(() -> new ResourceNotFoundException("Training sample not found"));
        trainingDataRepositoryPort.delete(sample);
        // 저장소 삭제가 실패하면 예외 → DB 삭제도 롤백되어 기록과 실제 파일이 어긋나지 않는다
        imageStoragePort.deleteDatasetObject(sample.getDatasetKey());
    }

    private TrainingSampleDto toDto(TrainingSample s) {
        return TrainingSampleDto.builder()
                .id(s.getId())
                .classificationId(s.getClassification().getId())
                .reportId(s.getReport() == null ? null : s.getReport().getId())
                .label(s.getLabel().name())
                .predictedLabel(s.getPredictedLabel().name())
                .modelVersion(s.getModelVersion())
                .datasetKey(s.getDatasetKey())
                .datasetUri(imageStoragePort.datasetUri(s.getDatasetKey()))
                .imageUrl(imageStoragePort.presignedUrlForKey(s.getDatasetKey()))
                .createdAt(s.getCreateDt())
                .build();
    }

    /** RFC 4180: 쉼표·따옴표·줄바꿈이 있으면 따옴표로 감싸고 내부 따옴표는 두 번 */
    private static String csv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
