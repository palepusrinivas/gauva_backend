package com.ridefast.ride_fast_backend.dto.cashback;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CashbackUsageRequest {
    
    /** Ride ID where cashback is being used */
    private Long rideId;
    
    /** Original fare amount */
    private BigDecimal fareAmount;
    
    /** Whether user wants to use cashback */
    private Boolean useCashback;
}

