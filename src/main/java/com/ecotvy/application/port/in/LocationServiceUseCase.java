package com.ecotvy.application.port.in;

import com.ecotvy.application.dto.LocationDisposalInfo;
import com.ecotvy.application.dto.DisposalPointDto;

import java.util.List;

public interface LocationServiceUseCase {
    LocationDisposalInfo getDisposalInfo(String city, String district, String dong, String wasteType);
    List<DisposalPointDto> getNearbyDisposalPoints(Double latitude, Double longitude, Double radiusKm);
}

