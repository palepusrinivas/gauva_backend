package com.ridefast.ride_fast_backend.repository.promo;

import com.ridefast.ride_fast_backend.model.promo.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CouponRepository extends JpaRepository<Coupon, Long> {
    Optional<Coupon> findByCodeIgnoreCase(String code);
}


