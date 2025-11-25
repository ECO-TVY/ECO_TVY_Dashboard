package com.ecotvy.application.port.in;

import com.ecotvy.application.dto.AdminDashboardDto;
import com.ecotvy.application.dto.LocationRuleDto;
import com.ecotvy.application.dto.MisclassificationReviewDto;
import com.ecotvy.application.dto.AIModelDto;

import java.util.List;

public interface AdminUseCase {
    AdminDashboardDto getDashboard();
    List<LocationRuleDto> getLocationRules(String city, String district, String dong);
    LocationRuleDto createLocationRule(LocationRuleDto ruleDto);
    LocationRuleDto updateLocationRule(Long ruleId, LocationRuleDto ruleDto);
    void deleteLocationRule(Long ruleId);
    List<MisclassificationReviewDto> getPendingMisclassifications();
    void approveMisclassification(Long reportId, Boolean addToTrainingData);
    void rejectMisclassification(Long reportId, String reason);
    List<AIModelDto> getAIModels();
    AIModelDto activateAIModel(String version);
}

