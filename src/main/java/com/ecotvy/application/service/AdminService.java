package com.ecotvy.application.service;

import com.ecotvy.application.dto.*;
import com.ecotvy.application.port.in.AdminUseCase;
import com.ecotvy.application.port.out.AdminRepositoryPort;
import com.ecotvy.application.port.out.LocationRepositoryPort;
import com.ecotvy.infrastructure.exception.ResourceNotFoundException;
import com.ecotvy.domain.admin.AIModel;
import com.ecotvy.domain.admin.MisclassificationReport;
import com.ecotvy.domain.location.Location;
import com.ecotvy.domain.waste.WasteType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminService implements AdminUseCase {

    private final AdminRepositoryPort adminRepositoryPort;
    private final LocationRepositoryPort locationRepositoryPort;

    @Override
    @Transactional(readOnly = true)
    public AdminDashboardDto getDashboard() {
        Long totalUsers = adminRepositoryPort.countTotalUsers();
        Long todayClassifications = adminRepositoryPort.countTodayClassifications();
        String activeModelVersion = adminRepositoryPort.findActiveAIModel()
                .map(AIModel::getVersion)
                .orElse("N/A");
        Long pendingMisclassifications = (long) adminRepositoryPort.findPendingReports().size();

        return AdminDashboardDto.builder()
                .totalUsers(totalUsers)
                .todayClassifications(todayClassifications)
                .activeModelVersion(activeModelVersion)
                .pendingMisclassifications(pendingMisclassifications)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<LocationRuleDto> getLocationRules(String city, String district, String dong) {
        List<Location> locations = locationRepositoryPort.findByCityAndDistrictAndDong(city, district, dong);
        return locations.stream()
                .map(this::toLocationRuleDto)
                .collect(Collectors.toList());
    }

    @Override
    public LocationRuleDto createLocationRule(LocationRuleDto ruleDto) {
        Location location = Location.builder()
                .city(ruleDto.getCity())
                .district(ruleDto.getDistrict())
                .dong(ruleDto.getDong())
                .wasteType(WasteType.valueOf(ruleDto.getWasteType()))
                .disposalDay(ruleDto.getDisposalDay())
                .disposalTime(ruleDto.getDisposalTime())
                .disposalGuide(ruleDto.getDisposalGuide())
                .build();

        Location saved = locationRepositoryPort.save(location);
        return toLocationRuleDto(saved);
    }

    @Override
    public LocationRuleDto updateLocationRule(Long ruleId, LocationRuleDto ruleDto) {
        // TODO: Location 엔티티에 findById 메서드 추가 필요
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public void deleteLocationRule(Long ruleId) {
        locationRepositoryPort.deleteById(ruleId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MisclassificationReviewDto> getPendingMisclassifications() {
        List<MisclassificationReport> reports = adminRepositoryPort.findPendingReports();
        return reports.stream()
                .map(this::toMisclassificationReviewDto)
                .collect(Collectors.toList());
    }

    @Override
    public void approveMisclassification(Long reportId, Boolean addToTrainingData) {
        MisclassificationReport report = adminRepositoryPort.findReportById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found"));

        report.approve();
        adminRepositoryPort.save(report);

        // TODO: 학습 데이터 추가 로직
        if (addToTrainingData) {
            // AI 학습 데이터에 추가
        }
    }

    @Override
    public void rejectMisclassification(Long reportId, String reason) {
        MisclassificationReport report = adminRepositoryPort.findReportById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found"));

        report.reject();
        adminRepositoryPort.save(report);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AIModelDto> getAIModels() {
        List<AIModel> models = adminRepositoryPort.findAllAIModels();
        return models.stream()
                .map(this::toAIModelDto)
                .collect(Collectors.toList());
    }

    @Override
    public AIModelDto activateAIModel(String version) {
        // 기존 활성 모델 비활성화
        adminRepositoryPort.findActiveAIModel().ifPresent(model -> {
            model.deactivate();
            adminRepositoryPort.save(model);
        });

        // 새 모델 활성화
        AIModel model = adminRepositoryPort.findByVersion(version)
                .orElseThrow(() -> new ResourceNotFoundException("AI Model not found"));

        model.activate();
        AIModel saved = adminRepositoryPort.save(model);
        return toAIModelDto(saved);
    }

    private LocationRuleDto toLocationRuleDto(Location location) {
        return LocationRuleDto.builder()
                .id(location.getId())
                .city(location.getCity())
                .district(location.getDistrict())
                .dong(location.getDong())
                .wasteType(location.getWasteType().name())
                .disposalDay(location.getDisposalDay())
                .disposalTime(location.getDisposalTime())
                .disposalGuide(location.getDisposalGuide())
                .build();
    }

    private MisclassificationReviewDto toMisclassificationReviewDto(MisclassificationReport report) {
        return MisclassificationReviewDto.builder()
                .id(report.getId())
                .classificationId(report.getClassification().getId())
                .imageUrl(report.getClassification().getImageUrl())
                .aiClassifiedType(report.getClassification().getWasteType().name())
                .correctType(report.getCorrectType().name())
                .confidence(report.getClassification().getConfidence())
                .build();
    }

    private AIModelDto toAIModelDto(AIModel model) {
        return AIModelDto.builder()
                .id(model.getId())
                .version(model.getVersion())
                .modelUrl(model.getModelUrl())
                .isActive(model.getIsActive())
                .description(model.getDescription())
                .accuracy(model.getAccuracy())
                .build();
    }
}

