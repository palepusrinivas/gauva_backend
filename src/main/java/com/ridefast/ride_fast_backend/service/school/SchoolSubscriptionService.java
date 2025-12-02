package com.ridefast.ride_fast_backend.service.school;

import java.util.List;

import com.ridefast.ride_fast_backend.model.school.SchoolSubscription;
import com.ridefast.ride_fast_backend.model.school.SchoolSubscriptionPlan;

public interface SchoolSubscriptionService {

    // Admin methods
    SchoolSubscriptionPlan createPlan(SchoolSubscriptionPlan plan);

    List<SchoolSubscriptionPlan> getAllPlans();

    SchoolSubscriptionPlan updatePlan(Long id, SchoolSubscriptionPlan plan);

    void deletePlan(Long id);

    // School methods
    SchoolSubscription subscribe(Long institutionId, Long planId);

    SchoolSubscription getCurrentSubscription(Long institutionId);
}
