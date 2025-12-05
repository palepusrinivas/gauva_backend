package com.ridefast.ride_fast_backend.dto.intercity;

import com.ridefast.ride_fast_backend.enums.IntercityVehicleType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Request DTO for searching available intercity trips
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IntercityTripSearchRequest {
    
    /** Route ID (optional) */
    private Long routeId;
    
    /** Origin coordinates */
    @NotNull(message = "Pickup latitude is required")
    private Double pickupLatitude;
    
    @NotNull(message = "Pickup longitude is required")
    private Double pickupLongitude;
    
    /** Destination coordinates */
    @NotNull(message = "Drop latitude is required")
    private Double dropLatitude;
    
    @NotNull(message = "Drop longitude is required")
    private Double dropLongitude;
    
    /** Preferred vehicle type (optional) */
    private IntercityVehicleType vehicleType;
    
    /** Preferred departure time (optional) */
    private LocalDateTime preferredDeparture;
    
    /** Number of seats needed */
    @Builder.Default
    private Integer seatsNeeded = 1;
    
    /** Search radius in km for matching trips */
    @Builder.Default
    private Double searchRadiusKm = 5.0;
}

