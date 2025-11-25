package com.ecotvy.application.port.in;

import com.ecotvy.application.dto.ClassificationResult;
import org.springframework.web.multipart.MultipartFile;

public interface ClassifyWasteUseCase {
    ClassificationResult classifyWaste(Long userId, MultipartFile imageFile);
}

