package com.ridefast.ride_fast_backend.model.promo;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "coupons", indexes = {
        @Index(name = "idx_coupon_code", columnList = "code", unique = true)
})
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Coupon {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String code;

    // either PERCENT or FLAT
    @Column(nullable = false, length = 16)
    private String type;

    // percent: 0-100; flat: absolute value in currency
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal value;

    // optional minimum fare to apply
    @Column(precision = 12, scale = 2)
    private BigDecimal minFare;

    private LocalDateTime startsAt;
    private LocalDateTime endsAt;

    // total global usage allowed; null means unlimited
    private Integer maxRedemptions;
    // per user usage; null means unlimited
    private Integer maxRedemptionsPerUser;

    @Column(nullable = false)
    private Boolean active;
}


