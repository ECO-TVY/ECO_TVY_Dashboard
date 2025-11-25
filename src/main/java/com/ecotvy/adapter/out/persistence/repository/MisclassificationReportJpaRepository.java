package com.ecotvy.adapter.out.persistence.repository;

import com.ecotvy.domain.admin.MisclassificationReport;
import com.ecotvy.domain.admin.ReportStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MisclassificationReportJpaRepository extends JpaRepository<MisclassificationReport, Long> {
    List<MisclassificationReport> findByStatus(ReportStatus status);
}

