package com.ecotvy.adapter.out.external;

import com.ecotvy.application.port.out.ImageStoragePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import jakarta.annotation.PostConstruct;
import java.time.Duration;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class S3ImageStorageAdapter implements ImageStoragePort {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    @Value("${aws.s3.bucket-name:ecotvy-images}")
    private String bucketName;

    @Value("${aws.s3.region:ap-northeast-2}")
    private String region;

    @Value("${aws.s3.endpoint:}")
    private String endpoint;

    /** 서명 URL 유효기간. SigV4 최대 7일 */
    @Value("${aws.s3.presigned-url-ttl:1h}")
    private Duration presignedUrlTtl;

    @PostConstruct
    void validateTtl() {
        if (presignedUrlTtl.isNegative() || presignedUrlTtl.isZero() || presignedUrlTtl.compareTo(Duration.ofDays(7)) > 0) {
            throw new IllegalStateException("aws.s3.presigned-url-ttl은 0보다 크고 7일 이하여야 합니다: " + presignedUrlTtl);
        }
    }

    /** 객체의 공개 URL 접두사 (AWS: virtual-hosted 형식, S3 호환 저장소: path-style 형식) */
    private String urlPrefix() {
        if (StringUtils.hasText(endpoint)) {
            return endpoint.replaceAll("/+$", "") + "/" + bucketName + "/";
        }
        return String.format("https://%s.s3.%s.amazonaws.com/", bucketName, region);
    }

    @Override
    public void copyToDataset(String imageUrl, String datasetKey) {
        String sourceKey = extractKeyFromUrl(imageUrl);
        // 실패 시 예외를 그대로 던져 승인 트랜잭션도 함께 롤백되게 한다 (관리자가 재시도 가능)
        s3Client.copyObject(CopyObjectRequest.builder()
                .sourceBucket(bucketName)
                .sourceKey(sourceKey)
                .destinationBucket(bucketName)
                .destinationKey(datasetKey)
                .build());
    }

    @Override
    public void deleteDatasetObject(String datasetKey) {
        s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucketName).key(datasetKey).build());
    }

    @Override
    public String extensionOf(String imageUrl) {
        String key = extractKeyFromUrl(imageUrl);
        int slash = key.lastIndexOf('/');
        int dot = key.lastIndexOf('.');
        if (dot > slash && key.length() - dot <= 5) {
            return key.substring(dot).toLowerCase();
        }
        return ".jpg";
    }

    @Override
    public String datasetUri(String datasetKey) {
        return "s3://" + bucketName + "/" + datasetKey;
    }

    @Override
    public String uploadImage(MultipartFile file, String userId) {
        try {
            String fileName = generateFileName(userId, file.getOriginalFilename());
            String key = "waste/" + userId + "/" + fileName;

            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType(file.getContentType())
                    .build();

            s3Client.putObject(putObjectRequest,
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

            return urlPrefix() + key;
        } catch (IOException e) {
            log.error("Failed to upload image to S3", e);
            throw new RuntimeException("Image upload failed", e);
        }
    }

    @Override
    public void deleteImage(String imageUrl) {
        try {
            String key = extractKeyFromUrl(imageUrl);
            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            s3Client.deleteObject(deleteRequest);
        } catch (Exception e) {
            log.error("Failed to delete image from S3", e);
        }
    }

    @Override
    public byte[] resizeImage(byte[] imageBytes, int maxWidth, int maxHeight) {
        try {
            BufferedImage originalImage = ImageIO.read(new java.io.ByteArrayInputStream(imageBytes));
            if (originalImage == null) {
                throw new IOException("Invalid image format");
            }

            int originalWidth = originalImage.getWidth();
            int originalHeight = originalImage.getHeight();

            // 비율 유지하며 리사이징
            double widthRatio = (double) maxWidth / originalWidth;
            double heightRatio = (double) maxHeight / originalHeight;
            double ratio = Math.min(widthRatio, heightRatio);

            int newWidth = (int) (originalWidth * ratio);
            int newHeight = (int) (originalHeight * ratio);

            BufferedImage resizedImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = resizedImage.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(originalImage, 0, 0, newWidth, newHeight, null);
            g.dispose();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(resizedImage, "jpg", baos);
            return baos.toByteArray();
        } catch (IOException e) {
            log.error("Failed to resize image", e);
            throw new RuntimeException("Image resize failed", e);
        }
    }

    private String generateFileName(String userId, String originalFilename) {
        String extension = originalFilename != null && originalFilename.contains(".")
                ? originalFilename.substring(originalFilename.lastIndexOf("."))
                : ".jpg";
        return UUID.randomUUID().toString() + extension;
    }

    @Override
    public String presignedUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            return imageUrl;
        }
        String key = ownKeyOf(imageUrl);
        // 이 저장소가 아닌 외부 URL은 서명할 수 없으므로 그대로 둔다
        return key == null ? imageUrl : presignedUrlForKey(key);
    }

    @Override
    public String presignedUrlForKey(String key) {
        try {
            return s3Presigner.presignGetObject(GetObjectPresignRequest.builder()
                            .signatureDuration(presignedUrlTtl)
                            .getObjectRequest(GetObjectRequest.builder().bucket(bucketName).key(key).build())
                            .build())
                    .url()
                    .toString();
        } catch (Exception e) {
            // 서명은 로컬 계산이라 보통 실패하지 않지만(자격 증명 누락 등), 응답 전체를 깨뜨리지 않도록 null
            log.error("Failed to presign key: {}", key, e);
            return null;
        }
    }

    /** 이 버킷의 URL이면 키, 아니면 null */
    private String ownKeyOf(String imageUrl) {
        String prefix = urlPrefix();
        if (imageUrl.startsWith(prefix)) {
            return imageUrl.substring(prefix.length());
        }
        // 예전 형식(AWS 주소로 저장된 기록 등): 업로드 경로 규칙(waste/...)으로 판별
        int keyIndex = imageUrl.indexOf("/waste/");
        return keyIndex >= 0 ? imageUrl.substring(keyIndex + 1) : null;
    }

    private String extractKeyFromUrl(String imageUrl) {
        // URL에서 키 추출: https://bucket.s3.region.amazonaws.com/key
        String prefix = urlPrefix();
        if (imageUrl.startsWith(prefix)) {
            return imageUrl.substring(prefix.length());
        }
        // 다른 형식의 URL 처리
        int keyIndex = imageUrl.indexOf("/waste/");
        return keyIndex >= 0 ? imageUrl.substring(keyIndex + 1) : imageUrl;
    }
}

