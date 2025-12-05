package com.ridefast.ride_fast_backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Configurable service types (Car, Bike, Auto, etc.)
 * Admin can add, modify, enable/disable services
 */
@Entity
@Table(name = "service_configs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String serviceId;  // e.g., "BIKE", "CAR", "AUTO"

    @Column(nullable = false, length = 100)
    private String name;  // e.g., "Bike Taxi"

    @Column(length = 100)
    private String displayName;  // e.g., "Bike Taxi"

    @Column(length = 500)
    private String description;

    @Column(length = 50)
    private String icon;  // Emoji or icon name

    @Column(length = 500)
    private String iconUrl;  // URL to icon image

    @Column(nullable = false)
    @Builder.Default
    private Integer capacity = 1;

    @Column(nullable = false)
    @Builder.Default
    private Integer displayOrder = 0;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(length = 50)
    private String vehicleType;  // e.g., "two_wheeler", "four_wheeler"

    @Column(length = 50)
    private String estimatedArrival;  // e.g., "2-5 mins"

    // Fare configuration
    @Column
    @Builder.Default
    private Double baseFare = 0.0;

    @Column
    @Builder.Default
    private Double perKmRate = 0.0;

    @Column
    @Builder.Default
    private Double perMinRate = 0.0;

    @Column
    @Builder.Default
    private Double minimumFare = 0.0;

    @Column
    @Builder.Default
    private Double cancellationFee = 0.0;

    // Additional settings
    @Column
    @Builder.Default
    private Double maxDistance = 50.0;  // km

    @Column
    @Builder.Default
    private Integer maxWaitTime = 10;  // minutes

    @Column(length = 50)
    private String category;  // "economy", "premium", "luxury"

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

