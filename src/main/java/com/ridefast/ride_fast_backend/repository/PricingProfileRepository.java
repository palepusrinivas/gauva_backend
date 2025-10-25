package com.ridefast.ride_fast_backend.repository;

import com.ridefast.ride_fast_backend.model.PricingProfile;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PricingProfileRepository extends JpaRepository<PricingProfile, Long> {
  Optional<PricingProfile> findFirstByActiveTrue();
}
