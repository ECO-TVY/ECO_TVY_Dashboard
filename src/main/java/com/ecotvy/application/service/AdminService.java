package com.ecotvy.application.service;

import com.ecotvy.application.dto.*;
import com.ecotvy.application.port.in.AdminUseCase;
import com.ecotvy.application.port.in.TrainingDataUseCase;
import com.ecotvy.domain.admin.ReportStatus;
import com.ecotvy.application.port.out.AIClassificationPort;
import com.ecotvy.application.port.out.AdminRepositoryPort;
import com.ecotvy.application.port.out.CachePort;
import com.ecotvy.application.port.out.ImageStoragePort;
import com.ecotvy.application.port.out.LocationRepositoryPort;
import com.ecotvy.infrastructure.error.enums.ErrorCode;
import com.ecotvy.infrastructure.exception.BusinessException;
import com.ecotvy.infrastructure.exception.ResourceNotFoundException;
import com.ecotvy.domain.admin.AIModel;
import com.ecotvy.domain.admin.MisclassificationReport;
import com.ecotvy.domain.location.Location;
import com.ecotvy.domain.waste.WasteType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminService implements AdminUseCase {

    private final AdminRepositoryPort adminRepositoryPort;
    private final LocationRepositoryPort locationRepositoryPort;
    private final CachePort cachePort;
    private final TrainingDataUseCase trainingDataUseCase;
    private final ImageStoragePort imageStoragePort;
    private final AIClassificationPort aiClassificationPort;

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
        WasteType wasteType = validateRule(ruleDto);
        ensureNoDuplicateRule(ruleDto, null);

        Location location = Location.builder()
                .city(ruleDto.getCity().trim())
                .district(ruleDto.getDistrict().trim())
                .dong(ruleDto.getDong().trim())
                .wasteType(wasteType)
                .disposalDay(ruleDto.getDisposalDay().trim())
                .disposalTime(ruleDto.getDisposalTime())
                .disposalGuide(ruleDto.getDisposalGuide())
                .build();

        Location saved = locationRepositoryPort.save(location);
        evictRuleCache(saved);
        return toLocationRuleDto(saved);
    }

    @Override
    public LocationRuleDto updateLocationRule(Long ruleId, LocationRuleDto ruleDto) {
        Location location = locationRepositoryPort.findById(ruleId)
                .orElseThrow(() -> new ResourceNotFoundException("Location rule not found"));
        WasteType wasteType = validateRule(ruleDto);
        ensureNoDuplicateRule(ruleDto, ruleId);

        evictRuleCache(location); // 지역/종류가 바뀌는 경우 이전 키도 무효화
        location.update(ruleDto.getCity().trim(), ruleDto.getDistrict().trim(), ruleDto.getDong().trim(),
                wasteType, ruleDto.getDisposalDay().trim(), ruleDto.getDisposalTime(), ruleDto.getDisposalGuide());
        Location saved = locationRepositoryPort.save(location);
        evictRuleCache(saved);
        return toLocationRuleDto(saved);
    }

    @Override
    public void deleteLocationRule(Long ruleId) {
        Location location = locationRepositoryPort.findById(ruleId)
                .orElseThrow(() -> new ResourceNotFoundException("Location rule not found"));
        locationRepositoryPort.deleteById(ruleId);
        evictRuleCache(location);
    }

    private WasteType validateRule(LocationRuleDto dto) {
        if (isBlank(dto.getCity()) || isBlank(dto.getDistrict()) || isBlank(dto.getDong())
                || isBlank(dto.getDisposalDay()) || isBlank(dto.getWasteType())) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        try {
            return WasteType.valueOf(dto.getWasteType());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    /** 같은 지역·종류의 규칙이 2개 이상이면 분류 시 단건 조회가 실패하므로 중복을 막는다 */
    private void ensureNoDuplicateRule(LocationRuleDto dto, Long selfId) {
        locationRepositoryPort.findByCityAndDistrictAndDongAndWasteType(
                        dto.getCity().trim(), dto.getDistrict().trim(), dto.getDong().trim(), dto.getWasteType())
                .filter(existing -> !existing.getId().equals(selfId))
                .ifPresent(existing -> {
                    throw new BusinessException(ErrorCode.DATA_ALREADY_EXISTS);
                });
    }

    private void evictRuleCache(Location location) {
        cachePort.evict(CacheKeys.locationRule(location.getCity(), location.getDistrict(),
                location.getDong(), location.getWasteType().name()));
    }

    /** 이미 처리된 신고를 다시 승인/반려하지 못하게 (중복 클릭, 다른 관리자와 동시 처리) */
    private void ensurePending(MisclassificationReport report) {
        if (report.getStatus() != ReportStatus.PENDING) {
            throw new BusinessException(ErrorCode.OPERATION_NOT_ALLOWED, "이미 처리된 신고입니다");
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
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

        ensurePending(report);

        report.approve();
        adminRepositoryPort.save(report);

        // 같은 트랜잭션: 저장소 복사가 실패하면 승인도 롤백되어 관리자가 다시 시도할 수 있다
        if (Boolean.TRUE.equals(addToTrainingData)) {
            trainingDataUseCase.addFromReport(report);
        }
    }

    @Override
    public void rejectMisclassification(Long reportId, String reason) {
        MisclassificationReport report = adminRepositoryPort.findReportById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found"));
        ensurePending(report);

        report.reject(reason);
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
        AIModel model = adminRepositoryPort.findByVersion(version)
                .orElseThrow(() -> new ResourceNotFoundException("AI Model not found"));

        // 응답 없는 서버로 모든 분류 요청이 넘어가지 않도록, 바꾸기 전에 연결부터 확인
        if (!aiClassificationPort.isHealthy(model.getModelUrl())) {
            throw new BusinessException(ErrorCode.AI_SERVER_UNREACHABLE,
                    "모델 서버(" + model.getModelUrl() + ")가 응답하지 않아 활성화하지 않았습니다");
        }

        // 기존 활성 모델 비활성화 후 새 모델 활성화
        adminRepositoryPort.findActiveAIModel()
                .filter(active -> !active.getId().equals(model.getId()))
                .ifPresent(active -> {
                    active.deactivate();
                    adminRepositoryPort.save(active);
                });
        model.activate();
        AIModel saved = adminRepositoryPort.save(model);
        // 분류 서비스가 활성 모델을 1시간 캐시하므로 즉시 반영되도록 무효화
        cachePort.evict(CacheKeys.ACTIVE_MODEL);
        return toAIModelDto(saved);
    }

    @Override
    public AIModelDto registerAIModel(AIModelRequest request) {
        String version = request.getVersion() == null ? null : request.getVersion().trim();
        if (version == null || !MODEL_VERSION_PATTERN.matcher(version).matches()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE,
                    "버전은 영문·숫자로 시작하고 영문·숫자·. _ - 만 쓸 수 있습니다 (최대 50자)");
        }
        if (adminRepositoryPort.findByVersion(version).isPresent()) {
            throw new BusinessException(ErrorCode.DATA_ALREADY_EXISTS);
        }
        AIModel saved = adminRepositoryPort.save(AIModel.builder()
                .version(version)
                .modelUrl(normalizeModelUrl(request.getModelUrl()))
                .description(normalizeDescription(request.getDescription()))
                .accuracy(validateAccuracy(request.getAccuracy()))
                .isActive(false) // 등록만 하고, 연결 확인을 거치는 활성화는 별도로
                .build());
        return toAIModelDto(saved);
    }

    @Override
    public AIModelDto updateAIModel(String version, AIModelRequest request) {
        AIModel model = adminRepositoryPort.findByVersion(version)
                .orElseThrow(() -> new ResourceNotFoundException("AI Model not found"));
        String newUrl = normalizeModelUrl(request.getModelUrl());

        // 사용 중인 모델의 서버 주소를 바꾸면 즉시 모든 분류가 그쪽으로 가므로 연결부터 확인
        if (Boolean.TRUE.equals(model.getIsActive()) && !newUrl.equals(model.getModelUrl())
                && !aiClassificationPort.isHealthy(newUrl)) {
            throw new BusinessException(ErrorCode.AI_SERVER_UNREACHABLE,
                    "사용 중인 모델이라 새 서버(" + newUrl + ")의 응답을 확인해야 하는데 연결할 수 없습니다");
        }
        model.update(newUrl, normalizeDescription(request.getDescription()), validateAccuracy(request.getAccuracy()));
        AIModel saved = adminRepositoryPort.save(model);
        if (Boolean.TRUE.equals(saved.getIsActive())) {
            cachePort.evict(CacheKeys.ACTIVE_MODEL);
        }
        return toAIModelDto(saved);
    }

    @Override
    public void deleteAIModel(String version) {
        AIModel model = adminRepositoryPort.findByVersion(version)
                .orElseThrow(() -> new ResourceNotFoundException("AI Model not found"));
        if (Boolean.TRUE.equals(model.getIsActive())) {
            throw new BusinessException(ErrorCode.OPERATION_NOT_ALLOWED,
                    "사용 중인 모델은 삭제할 수 없습니다. 다른 모델을 먼저 활성화하세요");
        }
        // 분류 기록은 버전 문자열만 보관(FK 아님)하므로 삭제해도 기록은 남는다
        adminRepositoryPort.delete(model);
    }

    /** 헤더(X-Model-Version)와 URL 경로에 쓰이므로 안전한 문자만 허용 */
    private static final Pattern MODEL_VERSION_PATTERN = Pattern.compile("^[A-Za-z0-9][A-Za-z0-9._-]{0,49}$");

    private static String normalizeModelUrl(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "모델 서버 URL을 입력해 주세요");
        }
        String url = raw.trim().replaceAll("/+$", "");
        try {
            URI uri = new URI(url);
            boolean httpScheme = "http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme());
            if (!httpScheme || uri.getHost() == null || uri.getQuery() != null || uri.getFragment() != null
                    || uri.getUserInfo() != null || url.length() > 255) {
                throw new URISyntaxException(url, "unsupported");
            }
        } catch (URISyntaxException e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE,
                    "모델 서버 URL은 http(s)://호스트[:포트][/경로] 형식이어야 합니다");
        }
        return url;
    }

    private static String normalizeDescription(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String d = raw.trim();
        if (d.length() > 1000) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "설명은 1000자 이하로 입력해 주세요");
        }
        return d;
    }

    private static Double validateAccuracy(Double accuracy) {
        if (accuracy != null && (accuracy.isNaN() || accuracy < 0 || accuracy > 1)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "정확도는 0~1 사이 값이어야 합니다");
        }
        return accuracy;
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
                .imageUrl(imageStoragePort.presignedUrl(report.getClassification().getImageUrl()))
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

