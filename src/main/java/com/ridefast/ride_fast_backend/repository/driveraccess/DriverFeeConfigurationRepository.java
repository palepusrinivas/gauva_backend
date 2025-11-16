package com.ridefast.ride_fast_backend.repository.driveraccess;

import com.ridefast.ride_fast_backend.model.driveraccess.DriverFeeConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DriverFeeConfigurationRepository extends JpaRepository<DriverFeeConfiguration, Long> {
    Optional<DriverFeeConfiguration> findByVehicleTypeIgnoreCase(String vehicleType);
}


