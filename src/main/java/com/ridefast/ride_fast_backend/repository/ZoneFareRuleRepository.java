package com.ridefast.ride_fast_backend.repository;

import com.ridefast.ride_fast_backend.model.ZoneFareRule;
import com.ridefast.ride_fast_backend.model.PricingProfile;
import com.ridefast.ride_fast_backend.model.Zone;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ZoneFareRuleRepository extends JpaRepository<ZoneFareRule, Long> {
  Optional<ZoneFareRule> findFirstByPricingProfileAndZoneAndActiveTrue(PricingProfile profile, Zone zone);
}
