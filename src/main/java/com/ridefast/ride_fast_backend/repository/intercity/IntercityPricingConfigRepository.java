package com.ridefast.ride_fast_backend.repository.intercity;

import com.ridefast.ride_fast_backend.model.intercity.IntercityPricingConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IntercityPricingConfigRepository extends JpaRepository<IntercityPricingConfig, Long> {
    /**
     * Get the first (and should be only) pricing configuration
     * There should only be one pricing config in the system
     */
    IntercityPricingConfig findFirstByOrderByIdAsc();
}
