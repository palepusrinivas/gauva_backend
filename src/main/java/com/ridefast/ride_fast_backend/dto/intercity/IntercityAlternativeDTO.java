package com.ridefast.ride_fast_backend.dto.intercity;

import com.ridefast.ride_fast_backend.enums.IntercityVehicleType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for alternative vehicle suggestions when min seats not met
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IntercityAlternativeDTO {
    private IntercityVehicleType vehicleType;
    private String displayName;
    private String imageUrl;
    
    /** Price for this alternative */
    private BigDecimal price;
    private BigDecimal perSeatPrice;
    
    /** Reason for suggestion */
    private String reason;
    
    /** Available seats in existing trips */
    private Integer availableTrips;
    
    /** Estimated wait time */
    private Integer estimatedWaitMinutes;
    
    /** Whether switching is recommended */
    private Boolean isRecommended;
}

