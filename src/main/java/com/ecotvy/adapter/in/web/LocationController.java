package com.ecotvy.adapter.in.web;

import com.ecotvy.application.dto.DisposalPointDto;
import com.ecotvy.application.dto.LocationDisposalInfo;
import com.ecotvy.application.port.in.LocationServiceUseCase;
import com.ecotvy.infrastructure.dto.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/location")
@RequiredArgsConstructor
public class LocationController {

    private final LocationServiceUseCase locationServiceUseCase;

    @GetMapping("/disposal-info")
    public ResponseEntity<ApiResponse<LocationDisposalInfo>> getDisposalInfo(
            @RequestParam String city,
            @RequestParam String district,
            @RequestParam String dong,
            @RequestParam String wasteType) {
        LocationDisposalInfo result = locationServiceUseCase.getDisposalInfo(city, district, dong, wasteType);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/disposal-points")
    public ResponseEntity<ApiResponse<List<DisposalPointDto>>> getNearbyDisposalPoints(
            @RequestParam Double latitude,
            @RequestParam Double longitude,
            @RequestParam(defaultValue = "2.0") Double radiusKm) {
        List<DisposalPointDto> result = locationServiceUseCase.getNearbyDisposalPoints(
                latitude, longitude, radiusKm);
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}

