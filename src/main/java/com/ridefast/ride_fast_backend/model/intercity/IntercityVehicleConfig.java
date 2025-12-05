package com.ridefast.ride_fast_backend.model.intercity;

import com.ridefast.ride_fast_backend.enums.IntercityVehicleType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Configuration for each intercity vehicle type
 * Defines pricing, seat capacity, and minimum seat requirements
 */
@Entity
@Table(name = "intercity_vehicle_configs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IntercityVehicleConfig {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true)
    private IntercityVehicleType vehicleType;

    /** Display name for the vehicle type */
    @Column(nullable = false)
    private String displayName;

    /** Total price for the vehicle (full fare) */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalPrice;

    /** Maximum number of seats available */
    @Column(nullable = false)
    private Integer maxSeats;

    /** Minimum seats required to dispatch the trip */
    @Column(nullable = false)
    private Integer minSeats;

    /** Description for customer display */
    @Column(columnDefinition = "TEXT")
    private String description;

    /** Target customer segment */
    private String targetCustomer;

    /** Recommendation tag (e.g., "Best Value", "Fast & Comfort") */
    private String recommendationTag;

    /** Display order for sorting */
    @Column(nullable = false)
    private Integer displayOrder;

    /** Whether this vehicle type is active */
    @Column(nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    /** Image URL for the vehicle */
    private String imageUrl;

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

    /**
     * Calculate per-head price based on seats filled
     */
    public BigDecimal calculatePerHeadPrice(int seatsFilled) {
        if (seatsFilled <= 0) return totalPrice;
        return totalPrice.divide(BigDecimal.valueOf(seatsFilled), 2, java.math.RoundingMode.CEILING);
    }
}

