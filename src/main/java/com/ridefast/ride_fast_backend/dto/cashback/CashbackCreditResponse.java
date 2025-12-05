package com.ridefast.ride_fast_backend.dto.cashback;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CashbackCreditResponse {
    private Boolean success;
    private String message;
    
    /** Cashback entry details */
    private Long entryId;
    private BigDecimal amountCredited;
    private BigDecimal percentageApplied;
    private LocalDateTime expiresAt;
    
    /** For popup message */
    private String popupTitle;
    private String popupMessage;
    
    /** Utilisation info for next ride */
    private BigDecimal utilisationLimit;
    private String utilisationMessage; // "Use ₹15 on your next trip within 24 hours"
}

