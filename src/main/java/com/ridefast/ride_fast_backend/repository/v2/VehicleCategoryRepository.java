package com.ridefast.ride_fast_backend.repository.v2;

import com.ridefast.ride_fast_backend.model.v2.VehicleCategory;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VehicleCategoryRepository extends JpaRepository<VehicleCategory, String> {
  Optional<VehicleCategory> findByName(String name);
  Optional<VehicleCategory> findByType(String type);
}
