package com.ridefast.ride_fast_backend.repository;

import com.ridefast.ride_fast_backend.enums.ServiceType;
import com.ridefast.ride_fast_backend.model.PricingProfile;
import com.ridefast.ride_fast_backend.model.ServiceRate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServiceRateRepository extends JpaRepository<ServiceRate, Long> {
  Optional<ServiceRate> findByPricingProfileAndServiceType(PricingProfile profile, ServiceType type);
}
