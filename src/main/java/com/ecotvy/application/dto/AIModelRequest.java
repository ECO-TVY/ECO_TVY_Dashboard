package com.ecotvy.application.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** AI 모델 등록/수정 요청. 수정 시 version은 무시된다(버전은 변경 불가). */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AIModelRequest {
    private String version;
    private String modelUrl;
    private String description;
    /** 0.0 ~ 1.0, 선택 */
    private Double accuracy;
}
