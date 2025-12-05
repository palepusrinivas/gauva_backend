package com.ridefast.ride_fast_backend.model.cashback;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Admin-configurable cashback settings
 */
@Entity
@Table(name = "cashback_settings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CashbackSettings {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Master switch - ON/OFF */
    @Column(nullable = false)
    @Builder.Default
    private Boolean isEnabled = false;

    /** Cashback percentage (10-50%) */
    @Column(nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal cashbackPercentage = new BigDecimal("10.00");

    /** Per trip wallet utilisation limit (₹10-₹15) */
    @Column(nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal utilisationLimit = new BigDecimal("15.00");

    /** Cashback validity in hours (default 24) */
    @Column(nullable = false)
    @Builder.Default
    private Integer validityHours = 24;

    /** Max cashback credits per user per day (0 = unlimited) */
    @Column(nullable = false)
    @Builder.Default
    private Integer maxCreditsPerDay = 1;

    /** Festival mode - extra cashback percentage */
    @Column(precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal festivalExtraPercentage = BigDecimal.ZERO;

    /** Festival mode start date */
    private LocalDateTime festivalStartDate;

    /** Festival mode end date */
    private LocalDateTime festivalEndDate;

    /** Categories enabled for cashback (JSON array) */
    @Column(columnDefinition = "TEXT")
    private String enabledCategories; // JSON: ["PREMIUM_EXPRESS_CAR", "CAR_SHARE_POOLING", ...]

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
     * Get effective cashback percentage (including festival bonus if active)
     */
    public BigDecimal getEffectivePercentage() {
        if (isFestivalActive()) {
            return cashbackPercentage.add(festivalExtraPercentage != null ? festivalExtraPercentage : BigDecimal.ZERO);
        }
        return cashbackPercentage;
    }

    /**
     * Check if festival mode is currently active
     */
    public boolean isFestivalActive() {
        if (festivalStartDate == null || festivalEndDate == null) return false;
        LocalDateTime now = LocalDateTime.now();
        return now.isAfter(festivalStartDate) && now.isBefore(festivalEndDate);
    }
}

