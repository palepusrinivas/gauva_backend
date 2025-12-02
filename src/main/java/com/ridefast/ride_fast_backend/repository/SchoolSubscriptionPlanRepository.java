package com.ridefast.ride_fast_backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ridefast.ride_fast_backend.model.school.SchoolSubscriptionPlan;

@Repository
public interface SchoolSubscriptionPlanRepository extends JpaRepository<SchoolSubscriptionPlan, Long> {
}
