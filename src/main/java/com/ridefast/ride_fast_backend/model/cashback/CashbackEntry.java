package com.ridefast.ride_fast_backend.model.cashback;

import com.ridefast.ride_fast_backend.enums.CashbackStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Individual cashback entry with 24-hour expiry
 */
@Entity
@Table(name = "cashback_entries")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CashbackEntry {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** User who received the cashback */
    @Column(nullable = false)
    private String userId;

    /** Ride ID that generated this cashback */
    @Column(nullable = false)
    private Long rideId;

    /** Ride type/category */
    private String rideCategory;

    /** Original ride fare */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal rideFare;

    /** Cashback percentage applied */
    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal percentageApplied;

    /** Full cashback amount credited */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    /** Amount already used from this entry */
    @Column(nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal amountUsed = BigDecimal.ZERO;

    /** Remaining amount (amount - amountUsed) */
    @Column(nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal amountRemaining = BigDecimal.ZERO;

    /** Cashback status */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private CashbackStatus status = CashbackStatus.ACTIVE;

    /** When cashback was credited */
    @Column(nullable = false)
    private LocalDateTime createdAt;

    /** When cashback expires */
    @Column(nullable = false)
    private LocalDateTime expiresAt;

    /** When cashback was fully used (if applicable) */
    private LocalDateTime usedAt;

    /** When cashback expired (if applicable) */
    private LocalDateTime expiredAt;

    /** Festival bonus flag */
    @Builder.Default
    private Boolean isFestivalBonus = false;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (amountRemaining == null || amountRemaining.compareTo(BigDecimal.ZERO) == 0) {
            amountRemaining = amount;
        }
    }

    /**
     * Check if this entry is expired
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    /**
     * Check if this entry can be used
     */
    public boolean isUsable() {
        return status == CashbackStatus.ACTIVE && 
               !isExpired() && 
               amountRemaining.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * Use amount from this entry (partial usage)
     */
    public BigDecimal use(BigDecimal amountToUse) {
        if (!isUsable()) return BigDecimal.ZERO;
        
        BigDecimal actualUsage = amountToUse.min(amountRemaining);
        amountUsed = amountUsed.add(actualUsage);
        amountRemaining = amountRemaining.subtract(actualUsage);
        
        if (amountRemaining.compareTo(BigDecimal.ZERO) <= 0) {
            status = CashbackStatus.USED;
            usedAt = LocalDateTime.now();
        }
        
        return actualUsage;
    }

    /**
     * Mark as expired
     */
    public void markExpired() {
        if (status == CashbackStatus.ACTIVE) {
            status = CashbackStatus.EXPIRED;
            expiredAt = LocalDateTime.now();
        }
    }
}

