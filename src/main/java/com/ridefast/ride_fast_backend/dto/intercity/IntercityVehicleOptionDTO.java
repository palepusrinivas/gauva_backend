package com.ridefast.ride_fast_backend.dto.intercity;

import com.ridefast.ride_fast_backend.enums.IntercityVehicleType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO for displaying vehicle options to customers
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IntercityVehicleOptionDTO {
    private IntercityVehicleType vehicleType;
    private String displayName;
    private String description;
    private String imageUrl;
    
    /** Total vehicle price */
    private BigDecimal totalPrice;
    
    /** Maximum seats available */
    private Integer maxSeats;
    
    /** Minimum seats required for dispatch */
    private Integer minSeats;
    
    /** Current per-head price (for share pool) */
    private BigDecimal currentPerHeadPrice;
    
    /** Seats currently available */
    private Integer availableSeats;
    
    /** Seats already booked */
    private Integer seatsBooked;
    
    /** Seats remaining (same as availableSeats for clarity) */
    private Integer seatsRemaining;
    
    /** Total seats in vehicle */
    private Integer seatsTotal;
    
    /** Target customer description */
    private String targetCustomer;
    
    /** Recommendation tag (e.g., "Best Value") */
    private String recommendationTag;
    
    /** Whether this is a recommended option */
    private Boolean isRecommended;
    
    /** Estimated wait time in minutes */
    private Integer estimatedWaitMinutes;
    
    /** Route ID if applicable */
    private Long routeId;
    
    /** Estimated departure time */
    private LocalDateTime estimatedDeparture;
    
    /** Distance in kilometers */
    private Double distanceKm;
    
    /** Estimated duration in minutes */
    private Integer estimatedDurationMinutes;
}

