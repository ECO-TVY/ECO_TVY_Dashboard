package com.ecotvy.adapter.out.external;

import com.ecotvy.application.dto.AIClassificationResponse;
import com.ecotvy.application.port.out.AIClassificationPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class AIClassificationAdapter implements AIClassificationPort {

    private final WebClient.Builder webClientBuilder;

    @Value("${ai.model.server.url:http://localhost:8000}")
    private String aiServerUrl;

    @Value("${ai.model.timeout:1500}")
    private int timeoutMs;

    @Override
    public AIClassificationResponse classify(InputStream imageInputStream, String modelVersion) {
        try {
            WebClient webClient = webClientBuilder
                    .baseUrl(aiServerUrl)
                    .build();

            byte[] imageBytes = readAllBytes(imageInputStream);

            AIClassificationResponse response = webClient.post()
                    .uri("/predict")
                    .header("X-Model-Version", modelVersion)
                    .contentType(MediaType.IMAGE_JPEG)
                    .body(BodyInserters.fromValue(imageBytes))
                    .retrieve()
                    .bodyToMono(AIClassificationResponse.class)
                    .timeout(Duration.ofMillis(timeoutMs))
                    .block();

            if (response == null) {
                throw new RuntimeException("AI classification returned null");
            }

            return response;
        } catch (Exception e) {
            log.error("Failed to classify image via AI service", e);
            throw new RuntimeException("AI classification failed: " + e.getMessage(), e);
        }
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

