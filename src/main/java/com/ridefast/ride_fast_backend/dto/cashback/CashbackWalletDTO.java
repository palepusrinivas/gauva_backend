package com.ridefast.ride_fast_backend.dto.cashback;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CashbackWalletDTO {
    
    /** Total active cashback balance */
    private BigDecimal totalBalance;
    
    /** Maximum usable amount per trip */
    private BigDecimal utilisationLimit;
    
    /** Can user use cashback on next trip */
    private Boolean canUseCashback;
    
    /** Active cashback entries (with timers) */
    private List<CashbackEntryDTO> activeEntries;
    
    /** Count of active entries */
    private Integer activeCount;
    
    /** Soonest expiring entry time */
    private Long soonestExpirySeconds;
    private String soonestExpiryFormatted;
}

