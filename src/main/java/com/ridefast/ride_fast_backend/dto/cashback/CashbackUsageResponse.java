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
public class CashbackUsageResponse {
    private Boolean success;
    private String message;
    
    /** Original fare */
    private BigDecimal originalFare;
    
    /** Cashback amount used */
    private BigDecimal cashbackUsed;
    
    /** Final payable amount */
    private BigDecimal finalPayable;
    
    /** Remaining wallet balance */
    private BigDecimal remainingBalance;
}

