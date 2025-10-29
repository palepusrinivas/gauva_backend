package com.ridefast.ride_fast_backend.repository.v2;

import com.ridefast.ride_fast_backend.model.v2.TripFare;
import com.ridefast.ride_fast_backend.model.v2.ZoneV2;
import com.ridefast.ride_fast_backend.model.v2.VehicleCategory;
import java.util.Optional;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TripFareRepository extends JpaRepository<TripFare, String> {
  Optional<TripFare> findFirstByZoneAndVehicleCategory(ZoneV2 zone, VehicleCategory category);
  Page<TripFare> findAllByOrderByUpdatedAtDesc(Pageable pageable);
  List<TripFare> findByZone_Id(String zoneId);
  List<TripFare> findByVehicleCategory_Id(String categoryId);
  Optional<TripFare> findByZone_IdAndVehicleCategory_Id(String zoneId, String categoryId);
}
