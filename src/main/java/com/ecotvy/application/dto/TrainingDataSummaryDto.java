package com.ecotvy.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Map;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrainingDataSummaryDto {
    private long total;
    /** 모든 WasteType을 키로 포함 (샘플이 없으면 0) — 클래스 불균형 확인용 */
    private Map<String, Long> countsByLabel;
    /** 데이터셋 루트 (예: s3://ecotvy-images/training/) — 라벨별 폴더 구조 */
    private String datasetRoot;
}
