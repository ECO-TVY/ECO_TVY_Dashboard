package com.ecotvy.application.port.out;

import com.ecotvy.application.dto.AIClassificationResponse;

import java.io.InputStream;

public interface AIClassificationPort {

    /**
     * @param serverUrl 분류를 요청할 모델 서버 주소. null이면 설정(ai.model.server.url)의 기본 서버
     */
    AIClassificationResponse classify(InputStream imageInputStream, String modelVersion, String serverUrl);

    /** 모델 서버의 GET {serverUrl}/health 가 2xx로 응답하는지 */
    boolean isHealthy(String serverUrl);
}
