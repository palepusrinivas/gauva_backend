package com.ridefast.ride_fast_backend.repository.v2;

import com.ridefast.ride_fast_backend.model.v2.TripFare;
import com.ridefast.ride_fast_backend.model.v2.ZoneV2;
import com.ridefast.ride_fast_backend.model.v2.VehicleCategory;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TripFareRepository extends JpaRepository<TripFare, String> {
  Optional<TripFare> findFirstByZoneAndVehicleCategory(ZoneV2 zone, VehicleCategory category);
}
