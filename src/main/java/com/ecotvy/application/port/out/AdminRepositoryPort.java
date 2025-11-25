package com.ecotvy.application.port.out;

import com.ecotvy.domain.admin.AIModel;
import com.ecotvy.domain.admin.MisclassificationReport;

import java.util.List;
import java.util.Optional;

public interface AdminRepositoryPort {
    Optional<AIModel> findActiveAIModel();
    Optional<AIModel> findByVersion(String version);
    List<AIModel> findAllAIModels();
    AIModel save(AIModel aiModel);
    List<MisclassificationReport> findPendingReports();
    Optional<MisclassificationReport> findReportById(Long id);
    MisclassificationReport save(MisclassificationReport report);
    Long countTotalUsers();
    Long countTodayClassifications();
}

