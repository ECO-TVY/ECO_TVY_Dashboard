package com.ecotvy.application.service;

import com.ecotvy.application.dto.AIClassificationResponse;
import com.ecotvy.application.dto.ActiveModelInfo;
import com.ecotvy.application.dto.ClassificationResult;
import com.ecotvy.application.dto.DisposalPointDto;
import com.ecotvy.application.dto.LocationDisposalInfo;
import com.ecotvy.application.port.in.ClassifyWasteUseCase;
import com.ecotvy.application.port.in.LocationServiceUseCase;
import com.ecotvy.application.port.out.*;
import com.ecotvy.infrastructure.exception.ResourceNotFoundException;
import com.ecotvy.domain.user.User;
import com.ecotvy.domain.waste.Classification;
import com.ecotvy.domain.waste.WasteType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ClassifyWasteService implements ClassifyWasteUseCase {

    private final UserRepositoryPort userRepositoryPort;
    private final ClassificationRepositoryPort classificationRepositoryPort;
    private final AIClassificationPort aiClassificationPort;
    private final ImageStoragePort imageStoragePort;
    private final LocationRepositoryPort locationRepositoryPort;
    private final LocationServiceUseCase locationServiceUseCase;
    private final AdminRepositoryPort adminRepositoryPort;
    private final CachePort cachePort;

    private static final String DEFAULT_MODEL_VERSION = "v1.0.0";
    private static final int MAX_IMAGE_SIZE_MB = 3;
    private static final int MAX_WIDTH = 1024;
    private static final int MAX_HEIGHT = 1024;

    @Override
    public ClassificationResult classifyWaste(Long userId, MultipartFile imageFile) {
        // 1. 사용자 조회
        User user = userRepositoryPort.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // 2. 이미지 검증 및 리사이징
        byte[] imageBytes = validateAndResizeImage(imageFile);

        // 3. S3에 이미지 업로드
        String imageUrl = imageStoragePort.uploadImage(imageFile, userId.toString());

        try {
            // 4. 활성 AI 모델 조회 (버전 + 모델 서버 주소)
            ActiveModelInfo activeModel = getActiveModel();

            // 5. AI 분류 요청 — 활성 모델의 서버로
            AIClassificationResponse aiResponse = aiClassificationPort.classify(
                    new ByteArrayInputStream(imageBytes), activeModel.version(), activeModel.serverUrl());

            // 6. 분류 결과 저장
            Classification classification = Classification.builder()
                    .user(user)
                    .imageUrl(imageUrl)
                    .wasteType(WasteType.valueOf(aiResponse.getWasteType()))
                    .confidence(aiResponse.getConfidence())
                    .labels(aiResponse.getLabels())
                    .aiModelVersion(aiResponse.getModelVersion())
                    .build();

            // 7. 지역 배출 규정 조회 및 설정
            setDisposalInfo(classification, user);

            Classification savedClassification = classificationRepositoryPort.save(classification);

            // 8. 사용자 통계 업데이트
            user.incrementClassification();
            userRepositoryPort.save(user);

            // 9. 근처 배출 장소 조회 (좌표가 없는 계정은 생략)
            List<DisposalPointDto> nearbyPoints = (user.getLatitude() == null || user.getLongitude() == null)
                    ? List.of()
                    : locationServiceUseCase.getNearbyDisposalPoints(user.getLatitude(), user.getLongitude(), 2.0);

            // 10. 결과 반환
            return ClassificationResult.builder()
                    .classificationId(savedClassification.getId())
                    .wasteType(savedClassification.getWasteType().name())
                    .wasteTypeDisplayName(savedClassification.getWasteType().getDisplayName())
                    .confidence(savedClassification.getConfidence())
                    .labels(savedClassification.getLabels())
                    .imageUrl(imageStoragePort.presignedUrl(savedClassification.getImageUrl()))
                    .disposalGuide(savedClassification.getDisposalGuide())
                    .disposalDay(savedClassification.getDisposalDay())
                    .disposalTime(savedClassification.getDisposalTime())
                    .nearbyDisposalPoints(nearbyPoints)
                    .build();

        } catch (Exception e) {
            // 실패 시 이미지 삭제
            imageStoragePort.deleteImage(imageUrl);
            log.error("Failed to classify waste", e);
            throw new RuntimeException("AI classification failed", e);
        }
    }

    private byte[] validateAndResizeImage(MultipartFile imageFile) {
        try {
            // 파일 크기 검증
            if (imageFile.getSize() > MAX_IMAGE_SIZE_MB * 1024 * 1024) {
                byte[] resized = imageStoragePort.resizeImage(
                        imageFile.getBytes(), MAX_WIDTH, MAX_HEIGHT);
                return resized;
            }
            return imageFile.getBytes();
        } catch (IOException e) {
            throw new RuntimeException("Failed to process image", e);
        }
    }

    private ActiveModelInfo getActiveModel() {
        // 캐시에서 먼저 조회 (관리자가 활성 모델을 바꾸면 AdminService가 무효화)
        ActiveModelInfo cached = cachePort.get(CacheKeys.ACTIVE_MODEL, ActiveModelInfo.class);
        if (cached != null) {
            return cached;
        }

        // DB에서 조회. 활성 모델이 없으면 기본 버전 + 설정의 기본 서버(serverUrl=null)
        ActiveModelInfo active = adminRepositoryPort.findActiveAIModel()
                .map(model -> new ActiveModelInfo(model.getVersion(), model.getModelUrl()))
                .orElse(new ActiveModelInfo(DEFAULT_MODEL_VERSION, null));

        // 캐시에 저장 (1시간)
        cachePort.put(CacheKeys.ACTIVE_MODEL, active, 3600);
        return active;
    }

    private void setDisposalInfo(Classification classification, User user) {
        if (user.getAddress() == null || user.getAddress().isBlank()) {
            return; // 주소가 없는 계정은 지역 배출 규정 없이 분류 결과만 저장
        }
        String[] addressParts = user.getAddress().split(" ");
        if (addressParts.length >= 3) {
            String city = addressParts[0];
            String district = addressParts[1];
            String dong = addressParts[2];

            // 캐시 키 생성 (관리자가 규정 변경 시 같은 키로 무효화)
            String cacheKey = CacheKeys.locationRule(city, district, dong, classification.getWasteType().name());

            LocationDisposalInfo info = cachePort.get(cacheKey, LocationDisposalInfo.class);
            if (info == null) {
                // 규정이 없는 지역이어도 분류 자체는 성공해야 하므로 예외를 던지는 getDisposalInfo 대신
                // 저장소를 직접 조회한다. (@Transactional 메서드가 던진 예외는 catch해도
                // 바깥 트랜잭션을 rollback-only로 만들어 커밋 시 UnexpectedRollbackException이 남)
                info = locationRepositoryPort.findByCityAndDistrictAndDongAndWasteType(
                                city, district, dong, classification.getWasteType().name())
                        .map(location -> LocationDisposalInfo.builder()
                                .wasteType(location.getWasteType().name())
                                .disposalDay(location.getDisposalDay())
                                .disposalTime(location.getDisposalTime())
                                .disposalGuide(location.getDisposalGuide())
                                .build())
                        .orElse(null);
                if (info != null) {
                    cachePort.put(cacheKey, info, 3600); // 1시간 캐시
                }
            }

            if (info != null) {
                classification.setDisposalGuide(info.getDisposalGuide());
                classification.setDisposalDay(info.getDisposalDay());
                classification.setDisposalTime(info.getDisposalTime());
            }
        }
    }
}

