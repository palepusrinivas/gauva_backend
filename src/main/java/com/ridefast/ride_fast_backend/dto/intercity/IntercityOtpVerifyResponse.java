package com.ridefast.ride_fast_backend.dto.intercity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for OTP verification
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IntercityOtpVerifyResponse {
    
    private Boolean success;
    private String message;
    
    /** Booking details */
    private Long bookingId;
    private String bookingCode;
    private String passengerName;
    private String passengerPhone;
    
    /** Verification details */
    private Integer passengersVerified;
    private LocalDateTime verifiedAt;
    
    /** Trip onboarding summary */
    private Long tripId;
    private Integer totalSeatsBooked;
    private Integer totalPassengersOnboarded;
    private Integer pendingVerifications;
}

