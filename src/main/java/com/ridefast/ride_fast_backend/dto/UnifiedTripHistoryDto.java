package com.ridefast.ride_fast_backend.dto;

import com.ridefast.ride_fast_backend.enums.IntercityBookingStatus;
import com.ridefast.ride_fast_backend.enums.IntercityBookingType;
import com.ridefast.ride_fast_backend.enums.IntercityTripStatus;
import com.ridefast.ride_fast_backend.enums.IntercityVehicleType;
import com.ridefast.ride_fast_backend.enums.PaymentStatus;
import com.ridefast.ride_fast_backend.enums.RideStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.ridefast.ride_fast_backend.model.PaymentDetails;

/**
 * Unified DTO for trip history that combines both regular rides and intercity bookings
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UnifiedTripHistoryDto {
    // Common fields
    private String tripType; // "REGULAR" or "INTERCITY"
    private Long id; // Ride ID or Booking ID
    private String code; // Ride shortCode or Booking bookingCode
    private LocalDateTime startTime; // Ride startTime or Trip scheduledDeparture
    private LocalDateTime endTime; // Ride endTime or Trip actualArrival
    private LocalDateTime createdAt;
    
    // Location fields
    private String pickupAddress;
    private Double pickupLatitude;
    private Double pickupLongitude;
    private String dropAddress;
    private Double dropLatitude;
    private Double dropLongitude;
    
    // Status fields
    private RideStatus rideStatus; // For regular rides
    private IntercityBookingStatus bookingStatus; // For intercity bookings
    private IntercityTripStatus tripStatus; // For intercity trips
    
    // Amount fields
    private Double fare; // For regular rides
    private BigDecimal totalAmount; // For intercity bookings
    private BigDecimal perSeatAmount; // For intercity bookings
    
    // Payment fields
    private PaymentDetails paymentDetails; // For regular rides
    private PaymentStatus paymentStatus; // For intercity bookings
    private String razorpayOrderId; // For intercity bookings
    
    // Regular ride specific fields
    private UserResponse user; // For regular rides (when driver views)
    private DriverResponse driver; // For regular rides (when user views)
    private Double distance;
    private Long duration;
    private Integer otp; // For regular rides
    
    // Intercity specific fields
    private IntercityBookingType bookingType;
    private Integer seatsBooked;
    private IntercityVehicleType vehicleType;
    private String vehicleDisplayName;
    private Integer totalSeats;
    private Integer availableSeats;
    private Boolean otpVerified;
    private LocalDateTime otpVerifiedAt;
    private Integer passengersOnboarded;
    
    // Driver info for intercity
    private DriverResponse intercityDriver;
}
