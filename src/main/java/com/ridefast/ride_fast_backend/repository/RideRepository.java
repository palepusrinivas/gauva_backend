package com.ridefast.ride_fast_backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.ridefast.ride_fast_backend.model.Ride;

public interface RideRepository extends JpaRepository<Ride,Long>{
  Page<Ride> findByUser_Id(String userId, Pageable pageable);
  Page<Ride> findByDriver_Id(Long driverId, Pageable pageable);
  boolean existsByShortCode(String shortCode);
}
