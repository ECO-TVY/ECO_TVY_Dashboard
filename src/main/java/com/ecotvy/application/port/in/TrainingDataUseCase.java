package com.ecotvy.application.port.in;

import com.ecotvy.application.dto.TrainingDataSummaryDto;
import com.ecotvy.application.dto.TrainingSampleDto;
import com.ecotvy.domain.admin.MisclassificationReport;

import java.util.List;

public interface TrainingDataUseCase {

    /**
     * 승인된 오분류 신고를 학습 데이터로 등록한다. 이미 등록된 분류 기록이면 아무것도 하지 않는다.
     * 저장소 복사가 실패하면 예외를 던진다 (호출한 트랜잭션도 롤백).
     */
    void addFromReport(MisclassificationReport report);

    TrainingDataSummaryDto getSummary();

    List<TrainingSampleDto> getSamples(int page, int size);

    /** 학습 스크립트용 매니페스트 CSV */
    String exportManifestCsv();

    /** 품질이 나쁜 샘플을 데이터셋에서 제외 (DB 기록 + 저장소 객체 삭제) */
    void removeSample(Long sampleId);
}
