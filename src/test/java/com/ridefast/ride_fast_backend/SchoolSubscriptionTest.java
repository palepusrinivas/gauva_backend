package com.ridefast.ride_fast_backend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ridefast.ride_fast_backend.model.school.Institution;
import com.ridefast.ride_fast_backend.model.school.SchoolSubscription;
import com.ridefast.ride_fast_backend.model.school.SchoolSubscriptionPlan;
import com.ridefast.ride_fast_backend.repository.SchoolSubscriptionPlanRepository;
import com.ridefast.ride_fast_backend.repository.SchoolSubscriptionRepository;
import com.ridefast.ride_fast_backend.repository.school.InstitutionRepository;
import com.ridefast.ride_fast_backend.service.school.impl.SchoolSubscriptionServiceImpl;

@ExtendWith(MockitoExtension.class)
class SchoolSubscriptionTest {

    @Mock
    private SchoolSubscriptionPlanRepository planRepository;

    @Mock
    private SchoolSubscriptionRepository subscriptionRepository;

    @Mock
    private InstitutionRepository institutionRepository;

    @InjectMocks
    private SchoolSubscriptionServiceImpl subscriptionService;

    @Test
    void testCreatePlan() {
        SchoolSubscriptionPlan plan = SchoolSubscriptionPlan.builder()
                .name("Gold Plan")
                .amount(1000.0)
                .durationDays(30)
                .build();

        when(planRepository.save(any(SchoolSubscriptionPlan.class))).thenReturn(plan);

        SchoolSubscriptionPlan created = subscriptionService.createPlan(plan);
        assertEquals("Gold Plan", created.getName());
    }

    @Test
    void testSubscribe() {
        Institution institution = new Institution();
        institution.setId(1L);

        SchoolSubscriptionPlan plan = SchoolSubscriptionPlan.builder()
                .id(1L)
                .name("Gold Plan")
                .durationDays(30)
                .build();

        when(institutionRepository.findById(1L)).thenReturn(Optional.of(institution));
        when(planRepository.findById(1L)).thenReturn(Optional.of(plan));
        when(subscriptionRepository.save(any(SchoolSubscription.class))).thenAnswer(i -> i.getArguments()[0]);

        SchoolSubscription subscription = subscriptionService.subscribe(1L, 1L);

        assertNotNull(subscription);
        assertEquals("ACTIVE", subscription.getStatus());
        assertNotNull(subscription.getStartDate());
        assertNotNull(subscription.getEndDate());
    }
}
