package com.ridefast.ride_fast_backend.repository.intercity;

import com.ridefast.ride_fast_backend.enums.IntercityVehicleType;
import com.ridefast.ride_fast_backend.model.intercity.IntercityVehicleConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IntercityVehicleConfigRepository extends JpaRepository<IntercityVehicleConfig, Long> {
    
    Optional<IntercityVehicleConfig> findByVehicleType(IntercityVehicleType vehicleType);
    
    List<IntercityVehicleConfig> findByIsActiveTrueOrderByDisplayOrderAsc();
    
    boolean existsByVehicleType(IntercityVehicleType vehicleType);
}

