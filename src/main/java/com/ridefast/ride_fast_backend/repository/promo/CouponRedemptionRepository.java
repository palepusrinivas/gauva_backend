package com.ridefast.ride_fast_backend.repository.promo;

import com.ridefast.ride_fast_backend.model.MyUser;
import com.ridefast.ride_fast_backend.model.promo.Coupon;
import com.ridefast.ride_fast_backend.model.promo.CouponRedemption;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CouponRedemptionRepository extends JpaRepository<CouponRedemption, Long> {
    long countByCoupon(Coupon coupon);
    long countByCouponAndUser(Coupon coupon, MyUser user);
}


