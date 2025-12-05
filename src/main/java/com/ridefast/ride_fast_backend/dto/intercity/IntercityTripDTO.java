package com.ridefast.ride_fast_backend.dto.intercity;

import com.ridefast.ride_fast_backend.enums.IntercityTripStatus;
import com.ridefast.ride_fast_backend.enums.IntercityVehicleType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO for intercity trip details
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IntercityTripDTO {
    private Long tripId;
    private String tripCode;
    
    private IntercityVehicleType vehicleType;
    private String vehicleDisplayName;
    private String vehicleImageUrl;
    
    private IntercityTripStatus status;
    
    /** Location details */
    private String pickupAddress;
    private Double pickupLatitude;
    private Double pickupLongitude;
    
    private String dropAddress;
    private Double dropLatitude;
    private Double dropLongitude;
    
    /** Timing */
    private LocalDateTime scheduledDeparture;
    private LocalDateTime countdownExpiry;
    private Long countdownSecondsRemaining;
    
    /** Seat information */
    private Integer totalSeats;
    private Integer seatsBooked;
    private Integer availableSeats;
    private Integer minSeats;
    private Boolean minSeatsMet;
    
    /** Onboarding information */
    private Integer passengersOnboarded;
    private Integer pendingVerifications;
    
    /** Pricing */
    private BigDecimal totalPrice;
    private BigDecimal currentPerHeadPrice;
    private BigDecimal projectedPriceIfYouJoin;
    
    /** Message to display */
    private String priceMessage;
    
    /** Whether user can join this trip */
    private Boolean canJoin;
}

