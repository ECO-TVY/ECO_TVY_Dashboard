package com.ecotvy.adapter.out.persistence.repository;

import com.ecotvy.domain.location.Location;
import com.ecotvy.domain.waste.WasteType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LocationJpaRepository extends JpaRepository<Location, Long> {
    Optional<Location> findByCityAndDistrictAndDongAndWasteType(String city, String district, String dong, WasteType wasteType);
    List<Location> findByCityAndDistrictAndDong(String city, String district, String dong);
}

