package com.ecotvy.adapter.out.external;

import com.ecotvy.application.dto.AIClassificationResponse;
import com.ecotvy.application.port.out.AIClassificationPort;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class AIClassificationAdapter implements AIClassificationPort {

    private static final Duration HEALTH_TIMEOUT = Duration.ofSeconds(3);

    private final WebClient.Builder webClientBuilder;

    @Value("${ai.model.server.url:http://localhost:8000}")
    private String defaultServerUrl;

    @Value("${ai.model.timeout:1500}")
    private int timeoutMs;

    /**
     * 요청마다 절대 URL을 쓰는 단일 클라이언트.
     * (공유 WebClient.Builder에 baseUrl을 설정하면 빈 전체가 바뀌어 모델별 주소가 서로 섞일 수 있음)
     */
    private WebClient webClient;

    @PostConstruct
    void init() {
        this.webClient = webClientBuilder.clone().build();
    }

    @Override
    public AIClassificationResponse classify(InputStream imageInputStream, String modelVersion, String serverUrl) {
        String baseUrl = baseUrl(serverUrl);
        try {
            byte[] imageBytes = readAllBytes(imageInputStream);

            // FastAPI /predict는 multipart/form-data의 "file" 필드(UploadFile)로 받으므로
            // filename이 포함된 파트로 전송해야 함 (filename이 없으면 422)
            MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
            bodyBuilder.part("file", new ByteArrayResource(imageBytes) {
                @Override
                public String getFilename() {
                    return "image";
                }
            }).contentType(MediaType.APPLICATION_OCTET_STREAM);

            AIClassificationResponse response = webClient.post()
                    .uri(baseUrl + "/predict")
                    .header("X-Model-Version", modelVersion)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(BodyInserters.fromMultipartData(bodyBuilder.build()))
                    .retrieve()
                    .bodyToMono(AIClassificationResponse.class)
                    .timeout(Duration.ofMillis(timeoutMs))
                    .block();

            if (response == null) {
                throw new RuntimeException("AI classification returned null");
            }

            return response;
        } catch (Exception e) {
            log.error("Failed to classify image via AI service at {}", baseUrl, e);
            throw new RuntimeException("AI classification failed: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean isHealthy(String serverUrl) {
        String url = baseUrl(serverUrl) + "/health";
        try {
            webClient.get().uri(url)
                    .retrieve()
                    .toBodilessEntity()
                    .timeout(HEALTH_TIMEOUT)
                    .block();
            return true;
        } catch (Exception e) {
            log.warn("AI model server health check failed: {} ({})", url, e.getMessage());
            return false;
        }
    }

    private String baseUrl(String serverUrl) {
        String url = StringUtils.hasText(serverUrl) ? serverUrl : defaultServerUrl;
        return url.replaceAll("/+$", "");
    }

    private byte[] readAllBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int nRead;
        byte[] data = new byte[1024];
        while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        return buffer.toByteArray();
    }
}
