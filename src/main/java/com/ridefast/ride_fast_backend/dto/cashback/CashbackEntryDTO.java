package com.ridefast.ride_fast_backend.dto.cashback;

import com.ridefast.ride_fast_backend.enums.CashbackStatus;
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
public class CashbackEntryDTO {
    private Long id;
    private String rideCategory;
    private BigDecimal rideFare;
    private BigDecimal percentageApplied;
    private BigDecimal amount;
    private BigDecimal amountUsed;
    private BigDecimal amountRemaining;
    private CashbackStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private LocalDateTime usedAt;
    private LocalDateTime expiredAt;
    private Boolean isFestivalBonus;
    
    // Computed fields
    private Long expiresInSeconds;
    private String expiresInFormatted; // "17:45:10"
    private Boolean isExpiringSoon; // < 1 hour
}

