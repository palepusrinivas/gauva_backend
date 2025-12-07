package com.ridefast.ride_fast_backend.repository;

import com.ridefast.ride_fast_backend.model.DriverDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface DriverDetailsRepository extends JpaRepository<DriverDetails, Long> {
  // Find by driverId (String field) instead of user.id
  Optional<DriverDetails> findByDriverId(String driverId);
}
