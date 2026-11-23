package com.ecotvy.application.dto;

/**
 * 분류에 사용할 활성 모델 (캐시 저장용).
 * serverUrl이 null이면 설정의 기본 AI 서버를 사용한다.
 */
public record ActiveModelInfo(String version, String serverUrl) {
}
