package com.ridefast.ride_fast_backend.model.promo;

import com.ridefast.ride_fast_backend.model.MyUser;
import com.ridefast.ride_fast_backend.model.Ride;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "coupon_redemptions", indexes = {
        @Index(name = "idx_redemption_coupon_user", columnList = "coupon_id,user_id")
})
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CouponRedemption {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private Coupon coupon;

    @ManyToOne(optional = false)
    private MyUser user;

    @ManyToOne
    private Ride ride;

    @Column(nullable = false)
    private LocalDateTime redeemedAt;
}


