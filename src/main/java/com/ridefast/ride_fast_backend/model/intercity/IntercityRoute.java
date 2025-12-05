package com.ridefast.ride_fast_backend.model.intercity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Predefined intercity routes between cities/locations
 */
@Entity
@Table(name = "intercity_routes")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IntercityRoute {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Unique route code (e.g., "HYD-BLR") */
    @Column(nullable = false, unique = true)
    private String routeCode;

    /** Origin city/location name */
    @Column(nullable = false)
    private String originName;

    @Column(nullable = false)
    private Double originLatitude;

    @Column(nullable = false)
    private Double originLongitude;

    /** Destination city/location name */
    @Column(nullable = false)
    private String destinationName;

    @Column(nullable = false)
    private Double destinationLatitude;

    @Column(nullable = false)
    private Double destinationLongitude;

    /** Estimated distance in kilometers */
    @Column(nullable = false)
    private Double distanceKm;

    /** Estimated duration in minutes */
    @Column(nullable = false)
    private Integer durationMinutes;

    /** Price multiplier for this route (1.0 = use default vehicle prices) */
    @Column(precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal priceMultiplier = BigDecimal.ONE;

    /** Whether this route is active */
    @Column(nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    /** Whether reverse route is also available */
    @Column(nullable = false)
    @Builder.Default
    private Boolean bidirectional = true;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

