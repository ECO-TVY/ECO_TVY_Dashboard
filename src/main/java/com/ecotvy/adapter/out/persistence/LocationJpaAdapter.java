package com.ecotvy.adapter.out.persistence;

import com.ecotvy.application.port.out.LocationRepositoryPort;
import com.ecotvy.domain.location.DisposalPoint;
import com.ecotvy.domain.location.Location;
import com.ecotvy.domain.waste.WasteType;
import com.ecotvy.adapter.out.persistence.repository.DisposalPointJpaRepository;
import com.ecotvy.adapter.out.persistence.repository.LocationJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class LocationJpaAdapter implements LocationRepositoryPort {

    private final LocationJpaRepository locationJpaRepository;
    private final DisposalPointJpaRepository disposalPointJpaRepository;

    @Override
    public Optional<Location> findByCityAndDistrictAndDongAndWasteType(String city, String district, String dong, String wasteType) {
        return locationJpaRepository.findByCityAndDistrictAndDongAndWasteType(
                city, district, dong, WasteType.valueOf(wasteType));
    }

    @Override
    public List<Location> findByCityAndDistrictAndDong(String city, String district, String dong) {
        return locationJpaRepository.findByCityAndDistrictAndDong(city, district, dong);
    }

    @Override
    public Location save(Location location) {
        return locationJpaRepository.save(location);
    }

    @Override
    public void deleteById(Long id) {
        locationJpaRepository.deleteById(id);
    }

    @Override
    public List<DisposalPoint> findNearbyDisposalPoints(Double latitude, Double longitude, Double radiusKm) {
        // 간단한 구현: 모든 활성화된 배출 장소 반환 (실제로는 거리 계산 필요)
        return disposalPointJpaRepository.findByActiveTrue();
    }

    @Override
    public DisposalPoint save(DisposalPoint disposalPoint) {
        return disposalPointJpaRepository.save(disposalPoint);
    }
}

