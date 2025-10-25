package com.ridefast.ride_fast_backend.repository;

import com.ridefast.ride_fast_backend.model.CancellationFeeRule;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CancellationFeeRuleRepository extends JpaRepository<CancellationFeeRule, Long> {
}
