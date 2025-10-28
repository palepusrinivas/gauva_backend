package com.ridefast.ride_fast_backend.repository;

import com.ridefast.ride_fast_backend.model.PricingConfig;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PricingConfigRepository extends JpaRepository<PricingConfig, Long> {
}
