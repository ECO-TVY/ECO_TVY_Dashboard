package com.ecotvy.application.port.out;

import com.ecotvy.application.dto.AIClassificationResponse;

import java.io.InputStream;

public interface AIClassificationPort {
    AIClassificationResponse classify(InputStream imageInputStream, String modelVersion);
}

