package com.ecotvy.application.service;

import com.ecotvy.application.dto.DisposalPointDto;
import com.ecotvy.application.dto.LocationDisposalInfo;
import com.ecotvy.application.port.in.LocationServiceUseCase;
import com.ecotvy.application.port.out.LocationRepositoryPort;
import com.ecotvy.infrastructure.exception.ResourceNotFoundException;
import com.ecotvy.domain.location.DisposalPoint;
import com.ecotvy.domain.location.Location;
import com.ecotvy.domain.waste.WasteType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LocationService implements LocationServiceUseCase {

    private final LocationRepositoryPort locationRepositoryPort;

    @Override
    public LocationDisposalInfo getDisposalInfo(String city, String district, String dong, String wasteType) {
        Location location = locationRepositoryPort.findByCityAndDistrictAndDongAndWasteType(
                city, district, dong, WasteType.valueOf(wasteType))
                .orElseThrow(() -> new ResourceNotFoundException("Location rule not found"));

        return LocationDisposalInfo.builder()
                .wasteType(location.getWasteType().name())
                .disposalDay(location.getDisposalDay())
                .disposalTime(location.getDisposalTime())
                .disposalGuide(location.getDisposalGuide())
                .build();
    }

    @Override
    public List<DisposalPointDto> getNearbyDisposalPoints(Double latitude, Double longitude, Double radiusKm) {
        List<DisposalPoint> points = locationRepositoryPort.findNearbyDisposalPoints(
                latitude, longitude, radiusKm);

        return points.stream()
                .map(point -> {
                    double distance = calculateDistance(latitude, longitude,
                            point.getLatitude(), point.getLongitude());
                    return DisposalPointDto.builder()
                            .id(point.getId())
                            .name(point.getName())
                            .latitude(point.getLatitude())
                            .longitude(point.getLongitude())
                            .address(point.getAddress())
                            .description(point.getDescription())
                            .distanceKm(distance)
                            .build();
                })
                .collect(Collectors.toList());
    }

    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        // Haversine formula
        final int R = 6371; // Earth radius in km
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}

