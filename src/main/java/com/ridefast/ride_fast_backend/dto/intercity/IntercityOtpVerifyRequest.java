package com.ridefast.ride_fast_backend.dto.intercity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for OTP verification
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IntercityOtpVerifyRequest {
    
    /** Booking ID to verify */
    private Long bookingId;
    
    /** OTP provided by passenger */
    private Integer otp;
    
    /** Number of passengers actually boarding (optional, defaults to seatsBooked) */
    private Integer passengersBoarding;
}

