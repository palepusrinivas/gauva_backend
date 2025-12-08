package com.ridefast.ride_fast_backend.dto.intercity;

import com.ridefast.ride_fast_backend.enums.IntercityBookingType;
import com.ridefast.ride_fast_backend.enums.IntercityVehicleType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Request DTO for driver to publish an intercity trip
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DriverTripPublishRequest {
    
    @NotNull(message = "Booking type is required")
    private IntercityBookingType bookingType;  // SHARE_POOL or PRIVATE
    
    @NotNull(message = "Vehicle type is required")
    private IntercityVehicleType vehicleType;
    
    /** Route ID (optional if providing custom pickup/drop) */
    private Long routeId;
    
    /** From location */
    @NotNull(message = "Pickup address is required")
    private String pickupAddress;
    
    private Double pickupLatitude;
    private Double pickupLongitude;
    
    /** To location */
    @NotNull(message = "Drop address is required")
    private String dropAddress;
    
    private Double dropLatitude;
    private Double dropLongitude;
    
    /** Start time */
    @NotNull(message = "Scheduled departure is required")
    private LocalDateTime scheduledDeparture;
    
    /** Total fare for the trip */
    @NotNull(message = "Total fare is required")
    private BigDecimal totalFare;
    
    /** Number of seats available (for share pooling) */
    private Integer seats;
    
    /** Return trip option */
    @Builder.Default
    private Boolean returnTrip = false;
    
    /** Return trip departure time */
    private LocalDateTime returnTripDeparture;
    
    /** Night fare toggle */
    @Builder.Default
    private Boolean nightFareEnabled = false;
    
    /** Night fare multiplier (e.g., 1.2 for 20% extra) */
    private BigDecimal nightFareMultiplier;
    
    /** Distance in KM (editable) */
    private BigDecimal distanceKm;
    
    /** Premium notification option (paid feature) */
    @Builder.Default
    private Boolean premiumNotification = false;
}

