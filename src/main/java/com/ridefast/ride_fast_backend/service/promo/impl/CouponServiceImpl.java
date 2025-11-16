package com.ridefast.ride_fast_backend.service.promo.impl;

import com.ridefast.ride_fast_backend.model.MyUser;
import com.ridefast.ride_fast_backend.model.Ride;
import com.ridefast.ride_fast_backend.model.promo.Coupon;
import com.ridefast.ride_fast_backend.model.promo.CouponRedemption;
import com.ridefast.ride_fast_backend.repository.RideRepository;
import com.ridefast.ride_fast_backend.repository.UserRepository;
import com.ridefast.ride_fast_backend.repository.promo.CouponRedemptionRepository;
import com.ridefast.ride_fast_backend.repository.promo.CouponRepository;
import com.ridefast.ride_fast_backend.service.promo.CouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CouponServiceImpl implements CouponService {

    private final CouponRepository couponRepo;
    private final CouponRedemptionRepository redemptionRepo;
    private final UserRepository userRepo;
    private final RideRepository rideRepo;

    @Override
    public BigDecimal computeDiscount(String code, Long userId, BigDecimal baseFare) {
        Coupon c = couponRepo.findByCodeIgnoreCase(code).orElseThrow();
        if (c.getActive() == null || !c.getActive()) return BigDecimal.ZERO;
        LocalDateTime now = LocalDateTime.now();
        if (c.getStartsAt() != null && now.isBefore(c.getStartsAt())) return BigDecimal.ZERO;
        if (c.getEndsAt() != null && now.isAfter(c.getEndsAt())) return BigDecimal.ZERO;
        if (c.getMinFare() != null && baseFare.compareTo(c.getMinFare()) < 0) return BigDecimal.ZERO;
        if (c.getMaxRedemptions() != null && redemptionRepo.countByCoupon(c) >= c.getMaxRedemptions()) return BigDecimal.ZERO;
        MyUser u = userRepo.findById(String.valueOf(userId)).orElseThrow();
        if (c.getMaxRedemptionsPerUser() != null && redemptionRepo.countByCouponAndUser(c, u) >= c.getMaxRedemptionsPerUser()) return BigDecimal.ZERO;

        if ("PERCENT".equalsIgnoreCase(c.getType())) {
            BigDecimal pct = c.getValue();
            if (pct.compareTo(BigDecimal.ZERO) <= 0) return BigDecimal.ZERO;
            if (pct.compareTo(new BigDecimal("100")) > 0) pct = new BigDecimal("100");
            return baseFare.multiply(pct).divide(new BigDecimal("100"));
        } else {
            BigDecimal flat = c.getValue();
            if (flat.compareTo(BigDecimal.ZERO) <= 0) return BigDecimal.ZERO;
            return flat.min(baseFare);
        }
    }

    @Override
    @Transactional
    public void redeem(String code, Long userId, Long rideId) {
        Coupon c = couponRepo.findByCodeIgnoreCase(code).orElseThrow();
        MyUser u = userRepo.findById(String.valueOf(userId)).orElseThrow();
        Ride r = rideId == null ? null : rideRepo.findById(rideId).orElse(null);
        CouponRedemption cr = CouponRedemption.builder()
                .coupon(c)
                .user(u)
                .ride(r)
                .redeemedAt(LocalDateTime.now())
                .build();
        redemptionRepo.save(cr);
    }
}


