package com.ridefast.ride_fast_backend.repository;

import com.ridefast.ride_fast_backend.model.DriverDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface DriverDetailsRepository extends JpaRepository<DriverDetails, Long> {
  Optional<DriverDetails> findByUser_Id(Long userId);
}
