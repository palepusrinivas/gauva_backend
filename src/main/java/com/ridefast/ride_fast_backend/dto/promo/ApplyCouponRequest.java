package com.ridefast.ride_fast_backend.dto.promo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ApplyCouponRequest {
    @NotBlank
    private String code;
    @NotNull
    private Long userId;
    @NotNull
    private BigDecimal baseFare;
}


