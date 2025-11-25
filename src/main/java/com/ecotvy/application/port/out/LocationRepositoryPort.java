package com.ecotvy.application.port.out;

import com.ecotvy.domain.location.Location;
import com.ecotvy.domain.location.DisposalPoint;

import java.util.List;
import java.util.Optional;

public interface LocationRepositoryPort {
    Optional<Location> findByCityAndDistrictAndDongAndWasteType(String city, String district, String dong, String wasteType);
    List<Location> findByCityAndDistrictAndDong(String city, String district, String dong);
    Location save(Location location);
    void deleteById(Long id);
    List<DisposalPoint> findNearbyDisposalPoints(Double latitude, Double longitude, Double radiusKm);
    DisposalPoint save(DisposalPoint disposalPoint);
}

