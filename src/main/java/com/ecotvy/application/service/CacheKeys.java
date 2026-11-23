package com.ecotvy.application.service;

/**
 * 분류(ClassifyWasteService)가 쓰는 캐시와 관리자(AdminService)가 무효화하는 캐시의 키를 한곳에서 관리.
 */
final class CacheKeys {

    /** 활성 모델 {version, serverUrl}. (예전 키 ai:model:active:version은 문자열 값이라 섞이지 않게 새 키 사용) */
    static final String ACTIVE_MODEL = "ai:model:active";

    static String locationRule(String city, String district, String dong, String wasteType) {
        return String.format("location:%s:%s:%s:%s", city, district, dong, wasteType);
    }

    private CacheKeys() {
    }
}
