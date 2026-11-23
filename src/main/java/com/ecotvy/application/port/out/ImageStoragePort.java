package com.ecotvy.application.port.out;

import org.springframework.web.multipart.MultipartFile;

public interface ImageStoragePort {
    String uploadImage(MultipartFile file, String userId);
    void deleteImage(String imageUrl);
    byte[] resizeImage(byte[] imageBytes, int maxWidth, int maxHeight);

    /**
     * 업로드된 이미지를 학습 데이터셋 위치로 복사한다 (저장소 내부 복사, 다운로드 없음).
     *
     * @param imageUrl   uploadImage가 반환한 원본 이미지 URL
     * @param datasetKey 복사될 키 (예: training/PLASTIC/123.jpg)
     */
    void copyToDataset(String imageUrl, String datasetKey);

    /** 학습 데이터셋에서 객체 삭제 (없으면 무시) */
    void deleteDatasetObject(String datasetKey);

    /** 이미지 URL의 확장자 (예: ".jpg"), 알 수 없으면 ".jpg" */
    String extensionOf(String imageUrl);

    /** 데이터셋 키의 전체 위치 (예: s3://bucket/training/PLASTIC/123.jpg) */
    String datasetUri(String datasetKey);

    /**
     * 저장된 이미지 URL을 브라우저가 읽을 수 있는 서명 URL(유효기간 있음)로 바꾼다.
     * 이 저장소의 URL이 아니면 그대로, null이면 null을 반환한다.
     */
    String presignedUrl(String imageUrl);

    /** 저장소 키(예: training/PLASTIC/123.jpg)에 대한 서명 URL */
    String presignedUrlForKey(String key);
}
