package com.ridefast.ride_fast_backend.repository.v2;

import com.ridefast.ride_fast_backend.model.v2.VehicleBrand;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VehicleBrandRepository extends JpaRepository<VehicleBrand, String> {
  Optional<VehicleBrand> findByName(String name);
}
