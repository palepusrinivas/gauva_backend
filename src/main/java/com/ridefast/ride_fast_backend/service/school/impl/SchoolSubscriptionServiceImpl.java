package com.ridefast.ride_fast_backend.service.school.impl;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.ridefast.ride_fast_backend.model.school.Institution;
import com.ridefast.ride_fast_backend.model.school.SchoolSubscription;
import com.ridefast.ride_fast_backend.model.school.SchoolSubscriptionPlan;
import com.ridefast.ride_fast_backend.repository.SchoolSubscriptionPlanRepository;
import com.ridefast.ride_fast_backend.repository.SchoolSubscriptionRepository;
import com.ridefast.ride_fast_backend.repository.school.InstitutionRepository;
import com.ridefast.ride_fast_backend.service.school.SchoolSubscriptionService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SchoolSubscriptionServiceImpl implements SchoolSubscriptionService {

    private final SchoolSubscriptionPlanRepository planRepository;
    private final SchoolSubscriptionRepository subscriptionRepository;
    private final InstitutionRepository institutionRepository;

    @Override
    public SchoolSubscriptionPlan createPlan(SchoolSubscriptionPlan plan) {
        return planRepository.save(plan);
    }

    @Override
    public List<SchoolSubscriptionPlan> getAllPlans() {
        return planRepository.findAll();
    }

    @Override
    public SchoolSubscriptionPlan updatePlan(Long id, SchoolSubscriptionPlan plan) {
        SchoolSubscriptionPlan existing = planRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Plan not found"));
        existing.setName(plan.getName());
        existing.setAmount(plan.getAmount());
        existing.setDurationDays(plan.getDurationDays());
        existing.setDescription(plan.getDescription());
        existing.setIsActive(plan.getIsActive());
        return planRepository.save(existing);
    }

    @Override
    public void deletePlan(Long id) {
        planRepository.deleteById(id);
    }

    @Override
    public SchoolSubscription subscribe(Long institutionId, Long planId) {
        Institution institution = institutionRepository.findById(institutionId)
                .orElseThrow(() -> new RuntimeException("Institution not found"));
        SchoolSubscriptionPlan plan = planRepository.findById(planId)
                .orElseThrow(() -> new RuntimeException("Plan not found"));

        SchoolSubscription subscription = SchoolSubscription.builder()
                .institution(institution)
                .plan(plan)
                .startDate(LocalDateTime.now())
                .endDate(LocalDateTime.now().plusDays(plan.getDurationDays()))
                .status("ACTIVE")
                .build();

        return subscriptionRepository.save(subscription);
    }

    @Override
    public SchoolSubscription getCurrentSubscription(Long institutionId) {
        return subscriptionRepository.findByInstitutionIdAndStatus(institutionId, "ACTIVE")
                .orElse(null);
    }
}
