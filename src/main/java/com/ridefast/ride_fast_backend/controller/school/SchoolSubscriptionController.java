package com.ridefast.ride_fast_backend.controller.school;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ridefast.ride_fast_backend.model.school.SchoolSubscription;
import com.ridefast.ride_fast_backend.model.school.SchoolSubscriptionPlan;
import com.ridefast.ride_fast_backend.service.school.SchoolSubscriptionService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/school/subscription")
@RequiredArgsConstructor
public class SchoolSubscriptionController {

    private final SchoolSubscriptionService subscriptionService;

    // Admin Endpoints

    @PostMapping("/plans")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SchoolSubscriptionPlan> createPlan(@RequestBody SchoolSubscriptionPlan plan) {
        return new ResponseEntity<>(subscriptionService.createPlan(plan), HttpStatus.CREATED);
    }

    @GetMapping("/plans")
    public ResponseEntity<List<SchoolSubscriptionPlan>> getAllPlans() {
        return new ResponseEntity<>(subscriptionService.getAllPlans(), HttpStatus.OK);
    }

    @PutMapping("/plans/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SchoolSubscriptionPlan> updatePlan(@PathVariable Long id,
            @RequestBody SchoolSubscriptionPlan plan) {
        return new ResponseEntity<>(subscriptionService.updatePlan(id, plan), HttpStatus.OK);
    }

    @DeleteMapping("/plans/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deletePlan(@PathVariable Long id) {
        subscriptionService.deletePlan(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    // School Endpoints

    @PostMapping("/{institutionId}/subscribe/{planId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SchoolSubscription> subscribe(@PathVariable Long institutionId, @PathVariable Long planId) {
        return new ResponseEntity<>(subscriptionService.subscribe(institutionId, planId), HttpStatus.OK);
    }

    @GetMapping("/{institutionId}/current")
    public ResponseEntity<SchoolSubscription> getCurrentSubscription(@PathVariable Long institutionId) {
        return new ResponseEntity<>(subscriptionService.getCurrentSubscription(institutionId), HttpStatus.OK);
    }
}
