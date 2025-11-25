package com.ecotvy.adapter.out.persistence;

import com.ecotvy.application.port.out.AdminRepositoryPort;
import com.ecotvy.domain.admin.AIModel;
import com.ecotvy.domain.admin.MisclassificationReport;
import com.ecotvy.adapter.out.persistence.repository.AIModelJpaRepository;
import com.ecotvy.adapter.out.persistence.repository.MisclassificationReportJpaRepository;
import com.ecotvy.adapter.out.persistence.repository.ClassificationJpaRepository;
import com.ecotvy.adapter.out.persistence.repository.UserJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class AdminJpaAdapter implements AdminRepositoryPort {

    private final AIModelJpaRepository aiModelJpaRepository;
    private final MisclassificationReportJpaRepository misclassificationReportJpaRepository;
    private final ClassificationJpaRepository classificationJpaRepository;
    private final UserJpaRepository userJpaRepository;

    @Override
    public Optional<AIModel> findActiveAIModel() {
        return aiModelJpaRepository.findByIsActiveTrue();
    }

    @Override
    public Optional<AIModel> findByVersion(String version) {
        return aiModelJpaRepository.findByVersion(version);
    }

    @Override
    public List<AIModel> findAllAIModels() {
        return aiModelJpaRepository.findAll();
    }

    @Override
    public AIModel save(AIModel aiModel) {
        return aiModelJpaRepository.save(aiModel);
    }

    @Override
    public List<MisclassificationReport> findPendingReports() {
        return misclassificationReportJpaRepository.findByStatus(
                com.ecotvy.domain.admin.ReportStatus.PENDING);
    }

    @Override
    public Optional<MisclassificationReport> findReportById(Long id) {
        return misclassificationReportJpaRepository.findById(id);
    }

    @Override
    public MisclassificationReport save(MisclassificationReport report) {
        return misclassificationReportJpaRepository.save(report);
    }

    @Override
    public Long countTotalUsers() {
        return userJpaRepository.count();
    }

    @Override
    public Long countTodayClassifications() {
        return classificationJpaRepository.countTodayClassifications();
    }
}

