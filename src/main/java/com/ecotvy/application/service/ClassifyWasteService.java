package com.ecotvy.application.service;

import com.ecotvy.application.dto.AIClassificationResponse;
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
            // 4. AI 모델 버전 조회
            String modelVersion = getActiveModelVersion();

            // 5. AI 분류 요청
            AIClassificationResponse aiResponse = aiClassificationPort.classify(
                    new ByteArrayInputStream(imageBytes), modelVersion);

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

            // 9. 근처 배출 장소 조회
            List<DisposalPointDto> nearbyPoints = locationServiceUseCase.getNearbyDisposalPoints(
                    user.getLatitude(), user.getLongitude(), 2.0);

            // 10. 결과 반환
            return ClassificationResult.builder()
                    .classificationId(savedClassification.getId())
                    .wasteType(savedClassification.getWasteType().name())
                    .wasteTypeDisplayName(savedClassification.getWasteType().getDisplayName())
                    .confidence(savedClassification.getConfidence())
                    .labels(savedClassification.getLabels())
                    .imageUrl(savedClassification.getImageUrl())
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

    private String getActiveModelVersion() {
        // 캐시에서 먼저 조회
        String cachedVersion = cachePort.get("ai:model:active:version", String.class);
        if (cachedVersion != null) {
            return cachedVersion;
        }

        // DB에서 조회
        String version = adminRepositoryPort.findActiveAIModel()
                .map(model -> model.getVersion())
                .orElse("v1.0.0");

        // 캐시에 저장 (1시간)
        cachePort.put("ai:model:active:version", version, 3600);
        return version;
    }

    private void setDisposalInfo(Classification classification, User user) {
        String[] addressParts = user.getAddress().split(" ");
        if (addressParts.length >= 3) {
            String city = addressParts[0];
            String district = addressParts[1];
            String dong = addressParts[2];

            // 캐시 키 생성
            String cacheKey = String.format("location:%s:%s:%s:%s",
                    city, district, dong, classification.getWasteType().name());

            LocationDisposalInfo info = cachePort.get(cacheKey, LocationDisposalInfo.class);
            if (info == null) {
                info = locationServiceUseCase.getDisposalInfo(
                        city, district, dong, classification.getWasteType().name());
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

