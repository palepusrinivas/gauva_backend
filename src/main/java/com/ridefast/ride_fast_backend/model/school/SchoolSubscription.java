package com.ridefast.ride_fast_backend.model.school;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "school_subscriptions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SchoolSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private Institution institution;

    @ManyToOne
    private SchoolSubscriptionPlan plan;

    private LocalDateTime startDate;

    private LocalDateTime endDate;

    private String status; // ACTIVE, EXPIRED, PENDING

    private String paymentId;
}
