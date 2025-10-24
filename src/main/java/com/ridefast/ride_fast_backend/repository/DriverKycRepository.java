package com.ridefast.ride_fast_backend.repository;

import com.ridefast.ride_fast_backend.model.DriverKyc;
import com.ridefast.ride_fast_backend.model.Driver;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DriverKycRepository extends JpaRepository<DriverKyc, Long> {
  Optional<DriverKyc> findByDriver(Driver driver);
}
