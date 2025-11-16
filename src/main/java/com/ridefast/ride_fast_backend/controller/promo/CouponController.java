package com.ridefast.ride_fast_backend.controller.promo;

import com.ridefast.ride_fast_backend.dto.promo.ApplyCouponRequest;
import com.ridefast.ride_fast_backend.model.promo.Coupon;
import com.ridefast.ride_fast_backend.repository.promo.CouponRepository;
import com.ridefast.ride_fast_backend.service.promo.CouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/promotions/coupons")
@RequiredArgsConstructor
public class CouponController {

    private final CouponRepository couponRepository;
    private final CouponService couponService;

    @GetMapping
    public ResponseEntity<List<Coupon>> list() {
        return ResponseEntity.ok(couponRepository.findAll());
    }

    @PostMapping
    public ResponseEntity<Coupon> create(@RequestBody @Validated Coupon body) {
        body.setId(null);
        if (body.getActive() == null) body.setActive(Boolean.TRUE);
        return new ResponseEntity<>(couponRepository.save(body), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Coupon> update(@PathVariable Long id, @RequestBody @Validated Coupon body) {
        Coupon existing = couponRepository.findById(id).orElseThrow();
        body.setId(existing.getId());
        return ResponseEntity.ok(couponRepository.save(body));
    }

    @PostMapping("/apply")
    public ResponseEntity<Map<String, Object>> apply(@RequestBody @Validated ApplyCouponRequest req) {
        BigDecimal discount = couponService.computeDiscount(req.getCode(), req.getUserId(), req.getBaseFare());
        BigDecimal finalFare = req.getBaseFare().subtract(discount);
        return ResponseEntity.ok(Map.of(
                "code", req.getCode(),
                "baseFare", req.getBaseFare(),
                "discount", discount,
                "finalFare", finalFare.max(BigDecimal.ZERO)
        ));
    }
}


