package com.ridefast.ride_fast_backend.repository;

import com.ridefast.ride_fast_backend.model.DriverDetails;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DriverDetailsRepository extends JpaRepository<DriverDetails, Long> {
}
