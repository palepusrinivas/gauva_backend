package com.ridefast.ride_fast_backend.dto.intercity;

import com.ridefast.ride_fast_backend.enums.IntercityBookingType;
import com.ridefast.ride_fast_backend.enums.IntercityVehicleType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Request DTO for creating an intercity booking
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IntercityBookingRequest {
    
    /** Route ID (optional if providing custom pickup/drop) */
    private Long routeId;
    
    @NotNull(message = "Vehicle type is required")
    private IntercityVehicleType vehicleType;
    
    @NotNull(message = "Booking type is required")
    private IntercityBookingType bookingType;
    
    /** Number of seats to book (for SHARE_POOL) */
    @Min(value = 1, message = "At least 1 seat required")
    @Builder.Default
    private Integer seatsToBook = 1;
    
    /** Existing trip ID to join (for SHARE_POOL) */
    private Long tripId;
    
    /** Pickup location */
    @NotNull(message = "Pickup address is required")
    private String pickupAddress;
    
    private Double pickupLatitude;
    private Double pickupLongitude;
    
    /** Drop location */
    @NotNull(message = "Drop address is required")
    private String dropAddress;
    
    private Double dropLatitude;
    private Double dropLongitude;
    
    /** Scheduled departure time */
    private LocalDateTime scheduledDeparture;
    
    /** Contact phone */
    private String contactPhone;
    
    /** Special instructions */
    private String specialInstructions;
    
    /** Passenger details for each seat */
    private List<PassengerDetail> passengers;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PassengerDetail {
        private String name;
        private String phone;
    }
}

