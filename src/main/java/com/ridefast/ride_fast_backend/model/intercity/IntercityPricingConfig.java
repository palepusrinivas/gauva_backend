package com.ridefast.ride_fast_backend.model.intercity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Configuration for intercity pricing settings
 * Stores commission percentage and other pricing parameters
 */
@Entity
@Table(name = "intercity_pricing_config")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IntercityPricingConfig {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /** Commission percentage (e.g., 5.0 for 5%) */
    @Column(nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal commissionPercent = new BigDecimal("5.00");
    
    /** Platform fee percentage (if applicable) */
    @Column(precision = 5, scale = 2)
    private BigDecimal platformFeePercent;
    
    /** GST percentage (e.g., 5.0 for 5%) */
    @Column(precision = 5, scale = 2)
    private BigDecimal gstPercent;
    
    /** Minimum commission amount (flat) */
    @Column(precision = 10, scale = 2)
    private BigDecimal minCommissionAmount;
    
    /** Maximum commission amount (flat) */
    @Column(precision = 10, scale = 2)
    private BigDecimal maxCommissionAmount;
    
    /** Night fare multiplier (e.g., 1.2 for 20% extra) */
    @Column(precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal nightFareMultiplier = new BigDecimal("1.20");
    
    /** Default price multiplier for routes (if not specified) */
    @Column(precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal defaultRoutePriceMultiplier = BigDecimal.ONE;
    
    /** Whether commission is enabled */
    @Column(nullable = false)
    @Builder.Default
    private Boolean commissionEnabled = true;
    
    /** Whether night fare is enabled globally */
    @Column(nullable = false)
    @Builder.Default
    private Boolean nightFareEnabled = true;
    
    /** Night fare start hour (24-hour format, e.g., 22 for 10 PM) */
    @Column
    @Builder.Default
    private Integer nightFareStartHour = 22;
    
    /** Night fare end hour (24-hour format, e.g., 6 for 6 AM) */
    @Column
    @Builder.Default
    private Integer nightFareEndHour = 6;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
