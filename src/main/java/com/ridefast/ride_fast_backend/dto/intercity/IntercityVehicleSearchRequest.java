package com.ridefast.ride_fast_backend.dto.intercity;

import com.ridefast.ride_fast_backend.enums.IntercityVehicleType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Request DTO for searching intercity vehicles
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IntercityVehicleSearchRequest {
    
    /** Route ID if known */
    private Long routeId;
    
    /** Pickup coordinates */
    private Double pickupLatitude;
    private Double pickupLongitude;
    
    /** Drop coordinates */
    private Double dropLatitude;
    private Double dropLongitude;
    
    /** Preferred vehicle type (optional filter) */
    private IntercityVehicleType vehicleType;
    
    /** Preferred departure time */
    private LocalDateTime preferredDeparture;
    
    /** Number of seats needed by customer */
    private Integer seatsNeeded;
    
    /** Search radius in kilometers */
    private Double searchRadiusKm;
}

