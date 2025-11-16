package com.ridefast.ride_fast_backend.service.promo;

import java.math.BigDecimal;

public interface CouponService {
    BigDecimal computeDiscount(String code, Long userId, BigDecimal baseFare);
    void redeem(String code, Long userId, Long rideId);
}


